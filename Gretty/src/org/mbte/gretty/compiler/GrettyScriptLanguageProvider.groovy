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

    private static final String GRETTY_DIR = "gretty";

    private static final String GRAILS_APP_DIR = "grails-app";

    Class<LanguageDefinition> findScriptLanguage(ModuleNode moduleNode) {
        List<ClassNode> classes = moduleNode.getClasses();
        if (!classes.size())
            return null

        def clazz = classes[0]
        if(!clazz.script)
            return null

        if(isGrettyScript(moduleNode.context)) {
            return GrettyContextLanguage
        }
    }

    protected boolean isGrettyScript(SourceUnit sourceNode) {
        def sourcePath = sourceNode.name
        File sourceFile = [sourcePath]
        def parent = sourceFile.parentFile
        while (parent) {
            def parentParent = parent.parentFile
            if (parent.name == GRETTY_DIR && parentParent && parentParent.name == GRAILS_APP_DIR) {
                return true
            }
            parent = parentParent
        }

        return false
    }
}