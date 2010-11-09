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
package org.mbte.groovypp.compiler.languages

import org.codehaus.groovy.ast.ModuleNode
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.expr.PropertyExpression

@Typed abstract class ScriptLanguageProvider {
    abstract Class<LanguageDefinition> findScriptLanguage(ModuleNode moduleNode)

    private static class Info extends LinkedHashMap<String,ScriptLanguageProvider>{
    }

    private static final WeakHashMap<ClassLoader,Info> providers = [:]
    private static final Info NONE = []

    static Collection<ScriptLanguageProvider> findProviders(ClassLoader gcl) {
        findProvidersInternal(gcl).values()
    }

    private static Info findProvidersInternal(ClassLoader gcl) {
        if(!gcl)
            return NONE

        synchronized(providers) {
            Info res = providers[gcl]
            if(res == null) {
                res = []
                def parentInfo = findProvidersInternal(gcl.parent)
                res.putAll parentInfo

                Map<String, URL> names = new LinkedHashMap<String, URL>();
                try {
                    def globalServices = gcl.getResources("META-INF/services/org.mbte.groovypp.compiler.Languages")
                    while (globalServices.hasMoreElements()) {
                        URL service = globalServices.nextElement();
                        String className;
                        BufferedReader svcIn = new BufferedReader(new InputStreamReader(service.openStream()));
                        try {
                            className = svcIn.readLine();
                        } catch (IOException ioe) {
                            continue;
                        }
                        while (className != null) {
                            if (!className.startsWith("#") && className.length() > 0) {
                                names.put(className, service);
                            }
                            try {
                                className = svcIn.readLine();
                            } catch (IOException ioe) {//
                            }
                        }
                    }
                } catch (IOException e) { //
                }

                for (name in names.keySet()) {
                    try {
                        def providerClass = gcl.loadClass(name)
                        def parentProviderClass = parentInfo[name]?.class
                        if(providerClass && parentProviderClass != providerClass) {
                            res [name] = providerClass.newInstance()
                        }
                    }
                    catch(e) {
                        //
                    }
                }

                providers[gcl] = res.empty ? NONE : res
            }
            return res
        }
    }
}
