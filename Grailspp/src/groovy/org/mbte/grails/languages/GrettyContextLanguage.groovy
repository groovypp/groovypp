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

import org.codehaus.groovy.ast.ClassHelper
import org.mbte.grails.compiler.GrailsScriptLanguageProvider
import org.codehaus.groovy.ast.expr.EmptyExpression
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
import org.mbte.grails.compiler.ArtefactCache

scriptLanguage: org.mbte.groovypp.compiler.languages.ScriptLanguageDefinition

interfaces: [ClassHelper.make(org.mbte.grails.languages.GrettyContextProvider)]

def superConversion = conversion
conversion = { moduleNode ->
    GrailsScriptLanguageProvider.improveGrailsPackage moduleNode, GrailsScriptLanguageProvider.GRETTY_ANCHOR
    superConversion.execute moduleNode
}

void handleStatement(ClassNode clazz, Statement statement, BlockStatement constructorCode) {
    if (!statement.statementLabel) {
        switch(statement) {
            case ExpressionStatement:
                def expr = statement.expression
                switch(expr) {
                    case DeclarationExpression:
                        def varExpr = (VariableExpression) expr.leftExpression
                        if(varExpr.getName().endsWith("Service")) {
                            def serviceName = "${varExpr.name[0].toUpperCase()}${varExpr.name.substring(1)}"
                            def arName = ArtefactCache.findArtefactClass(serviceName)
                            if(arName)
                                varExpr.setType(ClassHelper.make(arName))
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

    super.handleStatement(clazz, statement, constructorCode)
}
