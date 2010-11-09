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

import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.ast.ClassNode

/**
 * This is special language used to define other languages 
 */
class ScriptLanguageDefinition extends LanguageDefinition {
    ScriptLanguageDefinition () {
        baseClass = ClassHelper.make(ScriptLanguageDefinition)

        additionalImports = []
        additionalImports << ["org.codehaus.groovy.ast."]
        additionalImports << ["org.codehaus.groovy.ast.stmt."]
        additionalImports << ["org.codehaus.groovy.ast.expr."]
        additionalImports << ["org.objectweb.asm."]

        conversion = { moduleNode ->
            def clazz = moduleNode.classes[0]

            BlockStatement constructorCode = clazz.declaredConstructors[0].code
            BlockStatement runCode = clazz.getDeclaredMethod("run", Parameter.EMPTY_ARRAY).code
            for(int i = 0; i != runCode.statements.size(); ++i) {
                def statement = runCode.statements [i]
                handleStatement(clazz, statement, constructorCode)
            }
            runCode.statements.clear ()
        }
    }

    void handleStatement(ClassNode clazz, Statement statement, BlockStatement constructorCode) {
        if (statement.statementLabel) {
            switch (statement) {
                case ExpressionStatement:
                    moveLabeledExpressionStatement(statement, constructorCode)
                    break

                case BlockStatement:
                    moveLabeledBlockStatement(statement, constructorCode)
                    break
            }
        }
        else {
            constructorCode.statements << statement
        }
    }
}
