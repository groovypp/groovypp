package org.mbte.gretty.grails

import org.springframework.context.Lifecycle
import org.springframework.context.ApplicationContextAware
import org.springframework.context.ApplicationContext
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.DisposableBean
import org.codehaus.groovy.grails.commons.ConfigurationHolder
import org.codehaus.groovy.grails.plugins.support.aware.GrailsConfigurationAware
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAware
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.mbte.gretty.httpserver.GrettyServer
import org.mbte.gretty.httpserver.GrettyProxy

@Typed class GrettyBean implements InitializingBean, DisposableBean, GrailsApplicationAware {
    GrailsApplication grailsApplication
    
    GrettyProxy  proxy

    GrettyServer gretty

    void destroy() {
        proxy?.stop ()
        gretty?.stop ()
    }

    void afterPropertiesSet() {
        ConfigObject config = grailsApplication.config.gretty

        SocketAddress localAddress = config["localAddress"]

        SocketAddress proxyTo = config["proxyTo"]
        if(proxyTo) {
            proxy = [proxyTo]
            localAddress = localAddress ?: new InetSocketAddress(8081)
        }

        if(localAddress) {
            gretty = [
                localAddress: localAddress,

                default: {
                    proxy?.handle(request, response)
                }
            ]
        }

        gretty?.start ()
    }
}
