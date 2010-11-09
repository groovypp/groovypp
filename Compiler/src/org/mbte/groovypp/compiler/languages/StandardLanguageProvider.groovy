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
import org.codehaus.groovy.ast.expr.EmptyExpression
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.expr.PropertyExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.expr.Expression

@Typed class StandardLanguageProvider extends ScriptLanguageProvider {
    Class<LanguageDefinition> findScriptLanguage(ModuleNode moduleNode) {
        List<ClassNode> classes = moduleNode.getClasses();
        if (!classes.size())
            return null

        def clazz = classes[0]
        if(!clazz.script)
            return null

        Class<LanguageDefinition> scriptLanguageClass
        def runCode = clazz.getDeclaredMethod("run", Parameter.EMPTY_ARRAY).code
        if(runCode instanceof BlockStatement) {
            def first = ((BlockStatement)runCode).statements[0]
            if(first?.statementLabel && first.statementLabel.equals("scriptLanguage")) {
                if(first instanceof ExpressionStatement) {
                    ExpressionStatement fs = first
                    def lang = getScriptLanguage(fs.expression, null)
                    if(lang) {
                        try {
                            scriptLanguageClass = moduleNode.context.classLoader.loadClass(lang)
                        }
                        catch(e) {
                        }

                        if(!scriptLanguageClass) {
                            moduleNode.context.addError(["Failed to load script language '$lang'", first.lineNumber, first.columnNumber])
                            return
                        }

                        if(!LanguageDefinition.isAssignableFrom(scriptLanguageClass)) {
                            moduleNode.context.addError(["Script language class '$lang' must extend ${LanguageDefinition.name}", first.lineNumber, first.columnNumber])
                            return
                        }

                        fs.expression = EmptyExpression.INSTANCE
                        fs.statementLabel = null

                        return scriptLanguageClass
                    }
                    else {
                        moduleNode.context.addError(["scriptLanguage: format error", first.lineNumber, first.columnNumber])
                        return
                    }
                }
                else {
                    moduleNode.context.addError(["scriptLanguage: format error", first.lineNumber, first.columnNumber])
                    return
                }
            }
        }
    }

    static String getScriptLanguage(Expression expr, String tail = null) {
        switch(expr) {
            case VariableExpression:
                return tail ? "${expr.name}.$tail" : expr.name

            case PropertyExpression:
                return getScriptLanguage(expr.objectExpression, tail ? "${expr.propertyAsString}.$tail": expr.propertyAsString)

            default:
                return null
        }
    }
}
