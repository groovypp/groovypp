/*
 * Copyright 2009-2010 MBTE Sweden AB.
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

package org.mbte.groovypp.compiler.bytecode;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.syntax.Token;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.objectweb.asm.MethodVisitor;

public class UnresolvedPropertyBytecodeExpr extends ResolvedLeftExpr {
    private final BytecodeExpr object;
    private final BytecodeExpr prop;

    public UnresolvedPropertyBytecodeExpr(ASTNode parent, BytecodeExpr object, BytecodeExpr prop, CompilerTransformer compiler) {
        super(parent, ClassHelper.OBJECT_TYPE);
        this.object = object;
        this.prop = prop;
    }

    protected void compile(MethodVisitor mv) {
        object.visit(mv);
        box(object.getType(), mv);
        prop.visit(mv);
        box(prop.getType(), mv);
        mv.visitMethodInsn(INVOKESTATIC, "org/codehaus/groovy/runtime/InvokerHelper", "getProperty", "(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;");
    }

    public BytecodeExpr createAssign(ASTNode parent, final BytecodeExpr right, final CompilerTransformer compiler) {
        return new BytecodeExpr(parent, right.getType()) {
            protected void compile(MethodVisitor mv) {
                object.visit(mv);
                box(object.getType(), mv);
                prop.visit(mv);
                box(prop.getType(), mv);
                right.visit(mv);
                dup_x2(right.getType(), mv);
                box(right.getType(), mv);
                mv.visitMethodInsn(INVOKESTATIC, "org/codehaus/groovy/runtime/InvokerHelper", "setProperty", "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)V");
            }
        };
    }

    public BytecodeExpr createBinopAssign(ASTNode parent, Token method, final BytecodeExpr right, CompilerTransformer compiler) {
        return null;
    }

    public BytecodeExpr createPrefixOp(ASTNode exp, final int type, CompilerTransformer compiler) {
        return null;
    }

    public BytecodeExpr createPostfixOp(ASTNode exp, final int type, CompilerTransformer compiler) {
        return null;
    }
}
