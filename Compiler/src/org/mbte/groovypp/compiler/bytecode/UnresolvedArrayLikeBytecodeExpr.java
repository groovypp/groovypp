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

package org.mbte.groovypp.compiler.bytecode;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.PresentationUtil;
import org.mbte.groovypp.compiler.TypeUtil;
import org.objectweb.asm.MethodVisitor;

public class UnresolvedArrayLikeBytecodeExpr extends ResolvedLeftExpr {
    private final BytecodeExpr array;
    private final BytecodeExpr index;

    public UnresolvedArrayLikeBytecodeExpr(ASTNode parent, BytecodeExpr array, BytecodeExpr index, CompilerTransformer compiler) {
        super(parent, ClassHelper.OBJECT_TYPE);
        this.array = array;
        this.index = index;
    }

    protected void compile(MethodVisitor mv) {
        array.visit(mv);
        mv.visitLdcInsn("getAt");
        index.visit(mv);
        box(index.getType(), mv);
        mv.visitMethodInsn(INVOKESTATIC, "org/codehaus/groovy/runtime/ArrayUtil", "createArray", "(Ljava/lang/Object;)[Ljava/lang/Object;");
        mv.visitMethodInsn(INVOKESTATIC, "org/codehaus/groovy/runtime/InvokerHelper", "invokeMethod", "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/Object;");
    }

    public BytecodeExpr createAssign(ASTNode parent, final BytecodeExpr right, final CompilerTransformer compiler) {
        return new BytecodeExpr(parent, ClassHelper.OBJECT_TYPE) {
            protected void compile(MethodVisitor mv) {
                array.visit(mv);
                mv.visitLdcInsn("putAt");
                index.visit(mv);
                box(index.getType(), mv);
                right.visit(mv);
                box(right.getType(), mv);
                mv.visitMethodInsn(INVOKESTATIC, "org/codehaus/groovy/runtime/ArrayUtil", "createArray", "(Ljava/lang/Object;Ljava/lang/Object;)[Ljava/lang/Object;");
                mv.visitMethodInsn(INVOKESTATIC, "org/codehaus/groovy/runtime/InvokerHelper", "invokeMethod", "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/Object;");
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
