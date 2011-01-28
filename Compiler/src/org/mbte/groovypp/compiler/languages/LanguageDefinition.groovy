/*
 * Copyright 2009-2011 MBTE Sweden AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@Typed package org.mbte.groovypp.compiler.languages

import org.codehaus.groovy.ast.ModuleNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ImportNode
import org.codehaus.groovy.ast.stmt.EmptyStatement
import org.codehaus.groovy.ast.expr.VariableExpression

import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.mbte.groovypp.compiler.TypeUtil
import org.codehaus.groovy.syntax.Types
import org.codehaus.groovy.syntax.Token
import org.codehaus.groovy.ast.expr.BinaryExpression
import org.codehaus.groovy.ast.expr.PropertyExpression

import org.codehaus.groovy.ast.expr.EmptyExpression
import org.codehaus.groovy.ast.expr.ClosureExpression

import org.codehaus.groovy.ast.stmt.Statement

class LanguageDefinition {
    abstract static class ModuleOp {
        abstract void execute(ModuleNode module)
    }

    ClassNode    baseClass = ClassHelper.SCRIPT_TYPE
    ClassNode [] interfaces = []

    List<ImportNode> additionalImports

    ModuleOp conversion

    ModuleOp semanticAnalysis

    ModuleOp canonicalization

    ModuleOp instructionSelection

    void apply(ModuleNode moduleNode) {
        List<ClassNode> classes = moduleNode.getClasses();
        if (!classes.size())
            return

        def clazz = classes[0]
        if(!clazz.script)
            return

        improveScriptClass(clazz, moduleNode)
        conversion?.execute(moduleNode)
    }

    private void improveScriptClass(ClassNode clazz, ModuleNode moduleNode) {
        def run = clazz.getDeclaredMethod("run", Parameter.EMPTY_ARRAY)
        if (!(run.code instanceof BlockStatement)) {
            BlockStatement nbs = [[run.code], []]
            run.code = nbs
        }
        BlockStatement bs = run.code

        clazz.getDeclaredMethods("main")[0].code = EmptyStatement.INSTANCE

        // init(Binding) removed
        clazz.declaredConstructors.remove(1)

        clazz.addAnnotation([TypeUtil.TYPED])
        clazz.superClass = baseClass
        for (i in interfaces)
            clazz.addInterface i

        for (i in additionalImports) {
            if (i.static)
                moduleNode.addStaticImport(i.type, i.fieldName, i.alias)
            else
            if (i.alias)
                moduleNode.addImport(i.alias, i.type)
            else
                moduleNode.addStarImport(i.packageName)
        }
    }

    protected void moveStatement(Statement s, BlockStatement cbs) {
        cbs.statements << s
    }

    protected void moveLabeledBlockStatement(BlockStatement s, BlockStatement cbs) {
        def prop = new PropertyExpression(VariableExpression.THIS_EXPRESSION, s.statementLabel)
        prop.sourcePosition = s
        def closureBlock = new BlockStatement()
        closureBlock.sourcePosition = s
        closureBlock.statements.addAll(s.statements)
        ClosureExpression closure = [[], closureBlock]
        closure.sourcePosition = s
        BinaryExpression ne = [
                prop,
                Token.newSymbol(Types.ASSIGN, s.lineNumber, s.columnNumber),
                closure
        ]
        ne.sourcePosition = s
        s.statements.clear()
        s.statementLabel = null

        ExpressionStatement newStatement = [ne]
        newStatement.sourcePosition = ne
        cbs.statements << newStatement
    }

    protected void moveLabeledExpressionStatement(ExpressionStatement s, BlockStatement cbs) {
        def prop = new PropertyExpression(VariableExpression.THIS_EXPRESSION, s.statementLabel)
        prop.sourcePosition = s.expression
        BinaryExpression ne = [
                prop,
                Token.newSymbol(Types.ASSIGN, s.expression.lineNumber, s.expression.columnNumber),
                s.expression
        ]
        ne.sourcePosition = s.expression
        s.expression = EmptyExpression.INSTANCE
        s.statementLabel = null

        def newStatement = new ExpressionStatement(ne)
        newStatement.sourcePosition = ne
        cbs.statements << newStatement
    }
}
