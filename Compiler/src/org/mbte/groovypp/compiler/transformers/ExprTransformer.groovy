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

package org.mbte.groovypp.compiler.transformers


import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode

import org.mbte.groovypp.compiler.CompilerTransformer
import org.mbte.groovypp.compiler.RecordingVariableExpression
import org.mbte.groovypp.compiler.TypeUtil
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.codehaus.groovy.ast.expr.*
import org.mbte.groovypp.compiler.bytecode.*
import org.codehaus.groovy.classgen.BytecodeExpression
import org.mbte.groovypp.compiler.flow.OrExpression
import org.mbte.groovypp.compiler.flow.JumpIfExpression
import org.mbte.groovypp.compiler.flow.AndExpression
import org.mbte.groovypp.compiler.flow.ExpressionList
import org.mbte.groovypp.compiler.flow.LabelExpression

@Typed public abstract class ExprTransformer<T extends Expression> implements Opcodes {

    private static IdentityHashMap<Class, ExprTransformer> transformers = [:]

    static {
        transformers.put(CastExpression, new CastExpressionTransformer())
        transformers.put(ClassExpression, new ClassExpressionTransformer())
        transformers.put(ConstantExpression, new ConstantExpressionTransformer())
        transformers.put(ListExpression, new ListExpressionTransformer())
        transformers.put(MapExpression, new MapExpressionTransformer())
        transformers.put(SpreadExpression, new SpreadExpressionTransformer())
        transformers.put(VariableExpression, new VariableExpressionTransformer())
        transformers.put(RecordingVariableExpression, transformers.get(VariableExpression))
        transformers.put(DeclarationExpression, new DeclarationExpressionTransformer())
        transformers.put(ClosureExpression, new ClosureExpressionTransformer())
        transformers.put(MethodCallExpression, new MethodCallExpressionTransformer())
        transformers.put(PostfixExpression, new PostfixExpressionTransformer())
        transformers.put(PrefixExpression, new PrefixExpressionTransformer())
        transformers.put(PropertyExpression, new PropertyExpressionTransformer())
        transformers.put(BinaryExpression, new BinaryExpressionTransformer())
        transformers.put(GStringExpression, new GStringExpressionTransformer())
        transformers.put(ConstructorCallExpression, new ConstructorCallExpressionTransformer())
        transformers.put(RangeExpression, new RangeExpressionTransformer())
        transformers.put(FieldExpression, new FieldExpressionTransformer())
        transformers.put(UnaryMinusExpression, new UnaryMinusExpressionTransformer())
        transformers.put(UnaryPlusExpression, new UnaryPlusExpressionTransformer())
        transformers.put(ArrayExpression, new ArrayExpressionTransformer())
        transformers.put(BitwiseNegationExpression, new BitwiseNegationExpressionTransformer())
        transformers.put(AttributeExpression, new AttributeExpressionTransformer())
        transformers.put(NamedArgumentListExpression, new NamedArgumentListExpressionTransformer())
        transformers.put(MethodPointerExpression, new MethodPointerExpressionTransformer())
        transformers.put(EmptyExpression, new EmptyExpressionTransformer())
        transformers.put(JumpIfExpression, new JumpIfExpressionTransformer())
        transformers.put(ExpressionList, new ExpressionListTransformer())
        transformers.put(AndExpression, new AndExpressionTransformer())
        transformers.put(OrExpression, new OrExpressionTransformer())
        transformers.put(ExpressionList, new ExpressionListTransformer())
        transformers.put(LabelExpression, new LabelExpressionTransformer())

        def bool = new BooleanExpressionTransformer()
        transformers.put(BooleanExpression, bool)
        transformers.put(NotExpression, bool)

        def ternary = new TernaryExpressionTransformer()
        transformers.put(TernaryExpression, ternary)
        transformers.put(ElvisOperatorExpression, ternary)
    }

    public static Expression transformExpression(final Expression exp, CompilerTransformer compiler) {
        if (exp instanceof BytecodeExpression) {
            if (exp instanceof BytecodeExpr)
                return exp

            return new BytecodeExpr(exp, exp.getType()) {
                protected void compile(MethodVisitor mv) {
                    ((BytecodeExpression) exp).visit(mv)
                }
            }
        }

        def t = transformers.get(exp.getClass())
        t ? t.transform(exp, compiler) : compiler.transformImpl(exp)
    }

    public static BytecodeExpr transformLogicalExpression(Expression exp, CompilerTransformer compiler, Label label, boolean onTrue) {
        if (exp instanceof StaticMethodCallExpression) {
            StaticMethodCallExpression smce = exp
            MethodCallExpression mce = [new ClassExpression(smce.ownerType), smce.method, smce.arguments]

            mce.sourcePosition = smce
            return transformLogicalExpression(mce, compiler, label, onTrue)
        }

        def t = transformers.get(exp.class)
        t.transformLogical(exp, compiler, label, onTrue)
    }

    public abstract Expression transform(T exp, CompilerTransformer compiler)

    public BytecodeExpr transformLogical(T exp, CompilerTransformer compiler, final Label label, final boolean onTrue) {
        final BytecodeExpr be = transform(exp, compiler)
        final ClassNode resType = be.type

        if (resType === ClassHelper.VOID_TYPE) {
            return be
        }

        if (ClassHelper.isPrimitiveType(resType)) {
            return [
                'super': [exp, ClassHelper.VOID_TYPE],
                compile: { mv ->
                    be.visit(mv)
                    if (resType === ClassHelper.byte_TYPE
                            || resType === ClassHelper.short_TYPE
                            || resType === ClassHelper.char_TYPE
                            || resType === ClassHelper.boolean_TYPE
                            || resType === ClassHelper.int_TYPE) {
                    } else if (resType === ClassHelper.long_TYPE) {
                        mv.visitInsn(L2I)
                    } else if (resType === ClassHelper.float_TYPE) {
                        mv.visitInsn(F2I)
                    } else if (resType === ClassHelper.double_TYPE) {
                        mv.visitInsn(D2I)
                    }
                    mv.visitJumpInsn(onTrue ? IFNE : IFEQ, label)
                }
            ]
        }
        else {
            if (be.type == ClassHelper.OBJECT_TYPE) {
                return [
                    'super': [exp, ClassHelper.VOID_TYPE],

                    compile: { mv ->
                        be.visit(mv)
                        mv.visitMethodInsn(INVOKESTATIC, "org/mbte/groovypp/runtime/DefaultGroovyPPMethods", "asBooleanDynamic", "(Ljava/lang/Object;)Z")
                        mv.visitJumpInsn(onTrue ? IFNE : IFEQ, label)
                    }
                ]
            }

            def safeCall = new MethodCallExpression(new BytecodeExpr(exp, be.getType()) {
                protected void compile(MethodVisitor mv) {
                }
            }, "asBoolean", ArgumentListExpression.EMPTY_ARGUMENTS)
            safeCall.sourcePosition = exp

            final ResolvedMethodBytecodeExpr call = compiler.transform(safeCall)

            if (call.type != ClassHelper.boolean_TYPE)
                compiler.addError("asBoolean should return 'boolean'", exp)

            if (call.methodNode.declaringClass == ClassHelper.OBJECT_TYPE) {
                // fast path
                return new BytecodeExpr(exp, ClassHelper.VOID_TYPE) {
                    protected void compile(MethodVisitor mv) {
                        be.visit(mv)
                        mv.visitJumpInsn(onTrue ? IFNONNULL : IFNULL, label)
                    }
                }
            }
            else {
                return new BytecodeExpr(exp, ClassHelper.VOID_TYPE) {
                    protected void compile(MethodVisitor mv) {
                        be.visit(mv)
                        mv.visitInsn(DUP)
                        Label nullLabel = new Label(), endLabel = new Label()

                        mv.visitJumpInsn(IFNULL, nullLabel)

                        call.visit(mv)

                        if (onTrue) {
                            mv.visitJumpInsn(IFEQ, endLabel)
                            mv.visitJumpInsn(GOTO, label)

                            mv.visitLabel(nullLabel)
                            mv.visitInsn(POP)
                        }
                        else {
                            mv.visitJumpInsn(IFNE, endLabel)
                            mv.visitJumpInsn(GOTO, label)

                            mv.visitLabel(nullLabel)
                            mv.visitInsn(POP)
                            mv.visitJumpInsn(GOTO, label)
                        }

                        mv.visitLabel(endLabel)
                    }
                }
            }
        }
    }

    protected BytecodeExpr unboxReference(Expression parent, BytecodeExpr left, CompilerTransformer compiler) {
        def unboxing = TypeUtil.getReferenceUnboxingMethod(left.type)
        if (unboxing) {
            left = ResolvedMethodBytecodeExpr.create(parent, unboxing, left, new ArgumentListExpression(), compiler)
        }
        return left
    }
}
