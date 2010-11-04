/*
 * Copyright 2009-2010 MBTE Sweden AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mbte.grails

import groovypp.concurrent.CallLaterExecutors
import groovypp.concurrent.ResourcePool
import javax.servlet.FilterChain
import org.apache.commons.logging.Log
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAware
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.InitializingBean
import org.springframework.web.context.support.WebApplicationContextUtils
import org.springframework.web.filter.OncePerRequestFilter
import redis.clients.jedis.Jedis
import javax.servlet.http.*

class RedisSessionBean implements InitializingBean, DisposableBean, GrailsApplicationAware, HttpSessionListener {
    private static final ThreadLocal<RequestTLState> tls = []

    GrailsApplication grailsApplication

    ResourcePool<Jedis> jedis

    Log logger // initialized by filter

    void sessionCreated(HttpSessionEvent e) {
        if(jedis) {
            logger.debug "sessionCreated"
            def cookies = tls.get().request.getCookies()
            for(c in cookies) {
                if(c.name.equals("JSESSIONID") && c.value != e.session.id) {
                    logger.debug "created session is replacement for ${c.value}"
                    Map<String,?> loaded = jedis.executeSync {
                        try {
                            it.rename(c.value, e.session.id)
                        }
                        catch(err) { // noop
                        }
                        it.get(e.session.id)?.bytes?.fromSerialBytes()
                    }

                    if(loaded) {
                        for(kv in loaded.entrySet()) {
                            e.session.setAttribute(kv.key, kv.value)
                            logger.debug "loaded ${kv.key} : ${kv.value}"
                        }
                    }
                }
            }
        }
    }

    void sessionDestroyed(HttpSessionEvent e) {
        if(jedis) {
            logger.debug "sessionDestroyed"
            jedis.executeSync {
                try {
                    it.del e.session.id
                }
                catch(err) { // noop
                }
            }
        }
    }

    private static class RequestTLState {
        HttpServletRequest  request
        HttpServletResponse response

        RedisSessionBean bean
    }

    void afterPropertiesSet() {
        ConfigObject config = grailsApplication.config.redisSessionManager
        if(config) {
            String  host = config.host ?: 'localhost'
            Integer port = config.port ?: 6379
            Integer timeout = config.timeout ?: 0
            String  password = config.password

            jedis = [
                executor: CallLaterExecutors.newFixedThreadPool(10),
                initResources: {
                    (0..<10).map {
                        def j = new Jedis(host, port, timeout)
                        if(password)
                            j.auth(password)
                        j
                    }
                }
            ]
        }
    }

    void destroy() {
//        jedis?.destroy ()
    }

    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) {
//        if(!jedis) {
            filterChain.doFilter request, response
//        }
//        else {
//            tls.set([request:request, response:response])
//            try {
//                filterChain.doFilter(request, response)
//            }
//            finally {
//                HttpSession session = request.getSession(false)
//                if (session) {
//                    def attrs = [:]
//                    for (def en = session.attributeNames; en.hasMoreElements();) {
//                        String n = en.nextElement()
//                        attrs[n] = session.getAttribute(n)
//                    }
//
//                    jedis.executeSync {
//                        it.set(session.id, new String(attrs.toSerialBytes()))
//                        it.expire(session.id, session.getMaxInactiveInterval())
//                    }
//                }
//                tls.remove()
//            }
//        }
    }



    static class Filter extends OncePerRequestFilter {
        RedisSessionBean rsb

        protected void initFilterBean() {
            super.initFilterBean()
            rsb = WebApplicationContextUtils.getWebApplicationContext(servletContext).getBean(RedisSessionBean)
            rsb.logger = logger
        }

        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) {
            rsb.doFilterInternal(request, response, filterChain)
        }
    }

    static class Listener implements HttpSessionListener {
        void sessionCreated(HttpSessionEvent e) {
            WebApplicationContextUtils.getWebApplicationContext(e.session.servletContext).getBean(RedisSessionBean).sessionCreated(e)
        }

        void sessionDestroyed(HttpSessionEvent e) {
            WebApplicationContextUtils.getWebApplicationContext(e.session.servletContext).getBean(RedisSessionBean).sessionDestroyed(e)
        }
    }
}
