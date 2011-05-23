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

package org.mbte.groovypp.compiler.transformers;

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.expr.BooleanExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.NotExpression;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.StaticCompiler;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.mbte.groovypp.compiler.transformers.ExprTransformer;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class BooleanExpressionTransformer extends ExprTransformer<BooleanExpression> {
    public Expression transform(BooleanExpression exp, CompilerTransformer compiler) {
        if (exp instanceof NotExpression) {
            final Expression expr = exp.getExpression();
            if (expr instanceof NotExpression) {
                return compiler.transform(((NotExpression)expr).getExpression());
            }
            return transformNotExpression((NotExpression) exp, compiler);
        }
        else {
            return compiler.transform(exp.getExpression());
        }
    }

    public BytecodeExpr transformLogical(BooleanExpression exp, CompilerTransformer compiler, Label label, boolean onTrue) {
        if (exp instanceof NotExpression) {
            return compiler.transformLogical(exp.getExpression(), label, !onTrue);
        }
        else {
            return compiler.transformLogical(exp.getExpression(), label, onTrue);
        }
    }

    private Expression transformNotExpression(final NotExpression exp, CompilerTransformer compiler) {
        final BytecodeExpr internal = (BytecodeExpr) compiler.transform(exp.getExpression());
        return new NotBytecodeExpr(exp, internal);
    }

    private static class NotBytecodeExpr extends BytecodeExpr {
        private final BytecodeExpr internal;

        public NotBytecodeExpr(NotExpression exp, BytecodeExpr internal) {
            super(exp, ClassHelper.boolean_TYPE);
            this.internal = internal;
        }

        protected void compile(MethodVisitor mv) {
            mv.visitInsn(ICONST_0);
            internal.visit(mv);
            Label ok = new Label();
            // type non-primitive
            final ClassNode type = internal.getType();

            if (type == ClassHelper.Boolean_TYPE) {
                unbox(ClassHelper.boolean_TYPE, mv);
            } else {
                if (ClassHelper.isPrimitiveType(type)) {
                    // unwrapper - primitive
                    if (type == ClassHelper.byte_TYPE
                            || type == ClassHelper.short_TYPE
                            || type == ClassHelper.char_TYPE
                            || type == ClassHelper.int_TYPE) {
                    } else if (type == ClassHelper.long_TYPE) {
                        mv.visitInsn(L2I);
                    } else if (type == ClassHelper.float_TYPE) {
                        mv.visitInsn(F2I);
                    } else if (type == ClassHelper.double_TYPE) {
                        mv.visitInsn(D2I);
                    }
                } else {

                    mv.visitMethodInsn(INVOKESTATIC, StaticCompiler.DTT, "castToBoolean", "(Ljava/lang/Object;)Z");
                }
            }
            mv.visitJumpInsn(IFNE, ok);
            mv.visitInsn(POP);
            mv.visitInsn(ICONST_1);
            mv.visitLabel(ok);
        }
    }
}