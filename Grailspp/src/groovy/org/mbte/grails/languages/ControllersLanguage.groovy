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
package org.mbte.grails.languages

/**
 * action: {
 * }
 *
 * def why = []
 *
 * becomes
 *
 * class XxxController {
 *    def action = {
 *    }
 *
 *    def why = []
 * }
 *
 * all the rest of operations are prohibited
 }
 */

import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.expr.DeclarationExpression
import org.codehaus.groovy.ast.expr.ClosureExpression
import org.codehaus.groovy.ast.Parameter
import org.objectweb.asm.Opcodes
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.expr.EmptyExpression

import org.codehaus.groovy.ast.expr.ConstantExpression
import org.mbte.grails.compiler.GrailsScriptLanguageProvider

scriptLanguage: org.mbte.groovypp.compiler.languages.ScriptLanguageDefinition

interfaces: [ ClassHelper.make("org.mbte.grails.languages.ControllerMethods") ]

def superConversion = conversion
conversion = { moduleNode ->
    GrailsScriptLanguageProvider.improveGrailsPackage moduleNode, GrailsScriptLanguageProvider.CONTROLLERS_ANCHOR
    superConversion.execute moduleNode
}

void handleStatement(ClassNode clazz, Statement statement, BlockStatement constructorCode) {
    if (statement.statementLabel) {
        switch (statement) {
            case ExpressionStatement:
                switch(statement.statementLabel) {
                    case "defaultAction":
                        if(statement.expression instanceof VariableExpression) {
                            ConstantExpression ce = [((VariableExpression)statement.expression).name]
                            ce.sourcePosition = statement.expression
                            statement.expression = ce
                        }

                        def prop = clazz.addProperty(statement.statementLabel, Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC, ClassHelper.STRING_TYPE, statement.expression, null, null)
                        prop.sourcePosition = statement
                        statement.statementLabel = null
                        statement.expression = EmptyExpression.INSTANCE
                    return

                    case "allowedMethods":
                        def prop = clazz.addProperty(statement.statementLabel, Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC, ClassHelper.MAP_TYPE, statement.expression, null, null)
                        prop.sourcePosition = statement
                        statement.statementLabel = null
                        statement.expression = EmptyExpression.INSTANCE
                    return
                }

                def expr = statement.expression
                switch(expr) {
                    case ClosureExpression:
                        def prop = clazz.addProperty(statement.statementLabel, Opcodes.ACC_PUBLIC, ClassHelper.CLOSURE_TYPE, expr, null, null)
                        prop.sourcePosition = statement
                        statement.statementLabel = null
                        statement.expression = EmptyExpression.INSTANCE
                        return
                }
            break


            case BlockStatement:
                def closureCode = new BlockStatement()
                closureCode.statements.addAll(statement.statements)
                ClosureExpression closure = [Parameter.EMPTY_ARRAY, closureCode]
                closure.sourcePosition = statement

                def prop = clazz.addProperty(statement.statementLabel, Opcodes.ACC_PUBLIC, ClassHelper.CLOSURE_TYPE, closure, null, null)
                prop.sourcePosition = statement
                statement.statementLabel = null
                statement.statements.clear()
                return
        }
    }
    else {
        switch(statement) {
            case ExpressionStatement:
                def expr = statement.expression
                switch(expr) {
                    case DeclarationExpression:
                        def varExpr = (VariableExpression) expr.leftExpression
                        if(varExpr.getName().endsWith("Service")) {
                            varExpr.setType(ClassHelper.make("${varExpr.name[0].toUpperCase()}${varExpr.name.substring(1)}"))
                        }
                        def prop = clazz.addProperty(varExpr.name, Opcodes.ACC_PUBLIC, varExpr.getType(), expr.rightExpression, null, null)
                        prop.sourcePosition = statement
                        statement.statementLabel = null
                        statement.expression = EmptyExpression.INSTANCE
                        return
                }
            break
        }
    }
    clazz.module.context.addError(["Wrong controller script syntax", statement.lineNumber, statement.columnNumber])
}
