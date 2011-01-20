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





package org.mbte.groovypp.compiler

import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.syntax.SyntaxException
import org.codehaus.groovy.syntax.Token
import org.codehaus.groovy.syntax.Types
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.objectweb.asm.Opcodes
import org.codehaus.groovy.ast.*
import static org.codehaus.groovy.ast.ClassHelper.make
import org.codehaus.groovy.ast.expr.*

@Typed
@GroovyASTTransformation (phase = CompilePhase.CANONICALIZATION)
class CloneableASTTransform implements ASTTransformation, Opcodes {
    static final ClassNode CLONEABLE = make(Externalizable)

    void visit(ASTNode[] nodes, SourceUnit source) {
        ModuleNode module = nodes[0]
        for (ClassNode classNode: module.classes) {
            processClass classNode, source
        }
    }

    public static void processClass (ClassNode classNode, SourceUnit source) {
        for( c in classNode.innerClasses)
            processClass(c, source)
        
        if (!classNode.implementsInterface(CLONEABLE))
            return

        def method = classNode.getDeclaredMethod("clone", Parameter.EMPTY_ARRAY)
        if(!method) {
            classNode.addMethod("clone", Opcodes.ACC_PUBLIC, classNode, Parameter.EMPTY_ARRAY, ClassNode.EMPTY_ARRAY, new ExpressionStatement(
                    new MethodCallExpression(
                            VariableExpression.SUPER_EXPRESSION,
                            "clone",
                            new ArgumentListExpression()
                    )
            ))
        }
    }
}
