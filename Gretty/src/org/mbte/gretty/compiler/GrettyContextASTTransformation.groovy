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

import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.ModuleNode
import org.codehaus.groovy.ast.Parameter
import org.objectweb.asm.Opcodes
import org.codehaus.groovy.ast.ClassHelper
import org.mbte.groovypp.compiler.TypeUtil
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.AnnotatedNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ConstructorNode
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.mbte.groovypp.compiler.transformers.MethodCallExpressionTransformer
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.mbte.groovypp.compiler.transformers.VariableExpressionTransformer
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.stmt.EmptyStatement
import org.codehaus.groovy.ast.expr.EmptyExpression
import java.util.concurrent.ExecutorService

@GroovyASTTransformation(phase = CompilePhase.CONVERSION)
class GrettyContextASTTransformation implements ASTTransformation {
    static final ClassNode GRETTY_CONTEXT_PROVIDER = ClassHelper.make("org.mbte.gretty.compiler.GrettyContextProvider")
    static final ClassNode GRETTY_CONTEXT = ClassHelper.make("org.mbte.gretty.httpserver.GrettyContext")

    void visit(ASTNode[] nodes, SourceUnit source) {
        ASTNode astNode = nodes[0];

        if (!(astNode instanceof ModuleNode)) {
            return;
        }

        ModuleNode moduleNode = (ModuleNode) astNode;

        List<ClassNode> classes = moduleNode.getClasses();
        if (!classes.size())
            return

        def clazz = classes[0]
        if(!clazz.script)
            return

        def run = clazz.getDeclaredMethod("run", Parameter.EMPTY_ARRAY)
        BlockStatement bs = run.code instanceof BlockStatement ? run.code : null

        if(!bs || !bs.statements) {
            return
        }

        ExpressionStatement first = bs.statements[0] instanceof ExpressionStatement ? bs.statements[0] : null
        if(!first || !(first.expression instanceof MethodCallExpression) || ((MethodCallExpression)first.expression).methodAsString != 'language') {
            return
        }
        bs.statements[0] = EmptyStatement.INSTANCE

        run.addAnnotation(new AnnotationNode(TypeUtil.TYPED))
        clazz.addProperty("webContexts", Opcodes.ACC_PUBLIC, TypeUtil.withGenericTypes(ClassHelper.MAP_TYPE, ClassHelper.STRING_TYPE, GRETTY_CONTEXT), ConstantExpression.NULL, null, null)
        clazz.superClass = ClassHelper.OBJECT_TYPE
        clazz.addInterface GRETTY_CONTEXT_PROVIDER

        clazz.getMethods("main")[0].code = EmptyStatement.INSTANCE

        def constructors = clazz.getDeclaredConstructors()
        if(!constructors) {
            clazz.addConstructor(new ConstructorNode(Opcodes.ACC_PUBLIC, Parameter.EMPTY_ARRAY, ClassNode.EMPTY_ARRAY, new BlockStatement()))
            constructors = clazz.getDeclaredConstructors()
        }

        def c = constructors[0]
        def code = c.code
        def callRun = new ExpressionStatement(new MethodCallExpression(VariableExpression.THIS_EXPRESSION, "run", new ArgumentListExpression()))
        switch(code) {
            case BlockStatement:
                code.addStatement(callRun);
                break

            default:
                BlockStatement newCode = new BlockStatement()
                newCode.addStatement(code)
                newCode.addStatement(callRun)
                run.code = newCode
        }
    }
}
