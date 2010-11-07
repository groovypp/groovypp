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
package org.mbte.grails.compiler

import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.ModuleNode
import org.mbte.groovypp.compiler.languages.LanguageDefinition
import org.mbte.gretty.compiler.GrettyScriptLanguageProvider
import org.mbte.grails.languages.ControllersLanguage

@Typed public class GrailsScriptLanguageProvider extends GrettyScriptLanguageProvider {

    static final String CONTROLLERS_ANCHOR = "grails-app${File.separatorChar}controllers${File.separatorChar}"

    Class<LanguageDefinition> findScriptLanguage(ModuleNode moduleNode) {
        List<ClassNode> classes = moduleNode.getClasses();
        if (!classes.size())
            return null

        def clazz = classes[0]
        if(!clazz.script)
            return null

        if(isGrailsScript(moduleNode.context, CONTROLLERS_ANCHOR)) {
            return ControllersLanguage
        }

        return super.findScriptLanguage(moduleNode)
    }
}