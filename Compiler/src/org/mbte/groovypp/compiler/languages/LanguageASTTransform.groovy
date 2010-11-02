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
@Typed package org.mbte.groovypp.compiler.languages

import org.codehaus.groovy.transform.GroovyASTTransformation
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.control.SourceUnit

import org.codehaus.groovy.ast.ModuleNode

import org.mbte.groovypp.compiler.languages.LanguageDefinition
import org.codehaus.groovy.ast.ClassNode

abstract class LanguageASTTransform {

    @GroovyASTTransformation(phase = CompilePhase.CONVERSION)
    static class Conversion implements ASTTransformation {
        void visit(ASTNode[] nodes, SourceUnit source) {
            ASTNode astNode = nodes[0];

            if (!(astNode instanceof ModuleNode)) {
                return;
            }

            ModuleNode moduleNode = astNode

            List<ClassNode> classes = moduleNode.getClasses();
            if (!classes.size())
                return

            def clazz = classes[0]
            if(!clazz.script)
                return

            Class<LanguageDefinition> scriptLanguageClass
            for(i in moduleNode.imports) {
                if("scriptLanguage".equals(i.alias)) {
                    try {
                       scriptLanguageClass = moduleNode.context.classLoader.loadClass(i.className)
                       if(!(LanguageDefinition.isAssignableFrom(scriptLanguageClass)))
                           source.addError(["scriptLanguage class ${i.className} must extend org.mbte.groovypp.compiler.LanguageDefinition", i.lineNumber, i.columnNumber])
                    }
                    catch(e) {
                       source.addError(["Failed to load scriptLanguage class ${i.className}", i.lineNumber, i.columnNumber])
                       return
                    }
                    break
                }
            }

            scriptLanguageClass?.newInstance()?.apply(moduleNode)
        }
    }
}
