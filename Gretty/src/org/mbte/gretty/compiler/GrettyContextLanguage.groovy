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
@Typed(debug=true) package org.mbte.gretty.compiler

import org.codehaus.groovy.ast.ClassHelper

scriptLanguage: org.mbte.groovypp.compiler.languages.ScriptLanguageDefinition
                                     
interfaces: [ClassHelper.make(GrettyContextProvider)]

def superConversion = conversion
conversion = { moduleNode ->
    def packageNode = moduleNode.package
    if(!packageNode) {
        def pname = moduleNode.context.name
        def grettyPathAnchor = "grails-app${File.separatorChar}gretty/"
        def ind = pname.indexOf(grettyPathAnchor)
        if(ind != -1) {
            pname = pname.substring(ind + grettyPathAnchor.length())
            ind = pname.lastIndexOf(File.separator)
            if(ind != -1) {
                pname = pname.substring(0, ind).replace(File.separatorChar, '.')
                moduleNode.package = [pname]
                for(cls in moduleNode.classes) {
                    cls.setName("$pname.${cls.name}")
                }
            }
        }
    }
    superConversion.execute moduleNode
}