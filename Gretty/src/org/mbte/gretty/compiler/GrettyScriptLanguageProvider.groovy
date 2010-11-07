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
package org.mbte.gretty.compiler

import org.mbte.groovypp.compiler.languages.ScriptLanguageProvider
import org.mbte.groovypp.compiler.languages.LanguageDefinition
import org.codehaus.groovy.ast.ModuleNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.control.SourceUnit

@Typed public class GrettyScriptLanguageProvider extends ScriptLanguageProvider {

    static final String GRETTY_ANCHOR      = "grails-app${File.separatorChar}gretty${File.separatorChar}"

    Class<LanguageDefinition> findScriptLanguage(ModuleNode moduleNode) {
        List<ClassNode> classes = moduleNode.getClasses();
        if (!classes.size())
            return null

        def clazz = classes[0]
        if(!clazz.script)
            return null

        if(isGrailsScript(moduleNode.context, GRETTY_ANCHOR)) {
            return GrettyContextLanguage
        }
    }

    protected boolean isGrailsScript(SourceUnit sourceNode, String anchorPath) {
        def pname = sourceNode.name
        return pname.indexOf(anchorPath) != -1
    }

    static void improveGrailsPackage(ModuleNode moduleNode, String anchorPath) {
        def packageNode = moduleNode.package
        if(!packageNode) {
            def pname = moduleNode.context.name
            def ind = pname.indexOf(anchorPath)
            if(ind != -1) {
                pname = pname.substring(ind + anchorPath.length())
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
    }
}