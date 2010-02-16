package org.mbte.groovypp.compiler.transformers;

import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.expr.*;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.TypeUtil;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.objectweb.asm.MethodVisitor;

public class DeclarationExpressionTransformer extends ExprTransformer<DeclarationExpression> {
    public Expression transform(DeclarationExpression exp, final CompilerTransformer compiler) {
        if (!(exp.getLeftExpression() instanceof VariableExpression)) {
            compiler.addError("Variable name expected", exp);
        }

        final VariableExpression ve = (VariableExpression) exp.getLeftExpression();
        if (ve.getOriginType() != ve.getType())
            ve.setType(ve.getOriginType());

        if (!ve.isDynamicTyped()) {
            if (ClassHelper.isPrimitiveType(ve.getType()) && (exp.getRightExpression() instanceof ConstantExpression)) {
                ConstantExpression constantExpression = (ConstantExpression) exp.getRightExpression();
                if (constantExpression.getValue() == null) {
                    exp.setRightExpression(new ConstantExpression(0));
                }
            }
            CastExpression cast = new CastExpression(ve.getType(), exp.getRightExpression());
            cast.setSourcePosition(exp.getRightExpression());
            exp.setRightExpression(cast);
        }

        BytecodeExpr right = (BytecodeExpr) compiler.transform(exp.getRightExpression());
        if (right.getType() == TypeUtil.NULL_TYPE && ClassHelper.isPrimitiveType(ve.getType())) {
            final ConstantExpression cnst = new ConstantExpression(0);
            cnst.setSourcePosition(exp);
            right = (BytecodeExpr) compiler.transform(cnst);
        }

        if (hasFieldAnnotation(ve)) {
            ClassNode type;
            if (!ve.isDynamicTyped()) {
                type = ve.getType();
            } else {
                type = right.getType();
            }

            FieldNode fieldNode = compiler.classNode.addField(compiler.methodNode.getName() + "$" + ve.getName(), ACC_PRIVATE, type, exp.getRightExpression());
            compiler.context.setSelfInitialized(fieldNode);
            ve.setAccessedVariable(fieldNode);
            return new BytecodeExpr(exp, TypeUtil.NULL_TYPE) {
                protected void compile(MethodVisitor mv) {
                    mv.visitInsn(ACONST_NULL);
                }
            };
        }
        else {
            if (!ve.isDynamicTyped()) {
                if (ve.getType().equals(right.getType().redirect()) &&
                        ve.getType().getGenericsTypes() == null) {
                    ve.setType(right.getType());   //this is safe as long as generics are variant in type parameter.
                } else {
                    right = compiler.cast(right, ve.getType());
                }
                return new Static(exp, ve, right, compiler);
            } else {
                right = compiler.transformSynthetic(right);

                // let's try local type inference
                compiler.getLocalVarInferenceTypes().add(ve, right.getType());
                return new Dynamic(exp, right, compiler, ve);
            }
        }
    }

    public static boolean hasFieldAnnotation(VariableExpression ve) {
        for (AnnotationNode node : ve.getAnnotations()) {
            if ("Field".equals(node.getClassNode().getName()))
                return true;
            if ("groovy.lang.Field".equals(node.getClassNode().getName()))
                return true;
        }
        return false;
    }

    private static class Static extends BytecodeExpr {
        private final VariableExpression ve;
        private final BytecodeExpr right;
        private final CompilerTransformer compiler;

        public Static(DeclarationExpression exp, VariableExpression ve, BytecodeExpr right, CompilerTransformer compiler) {
            super(exp, ve.getType());
            this.ve = ve;
            this.right = right;
            this.compiler = compiler;
        }

        protected void compile(MethodVisitor mv) {
            right.visit(mv);
            box(right.getType(), mv);
            unbox(ve.getType(), mv);
            dup(ve.getType(), mv);
            compiler.compileStack.defineVariable(ve, true);
        }
    }

    private static class Dynamic extends BytecodeExpr {
        private final BytecodeExpr right;
        private final CompilerTransformer compiler;
        private final VariableExpression ve;

        public Dynamic(DeclarationExpression exp, BytecodeExpr right, CompilerTransformer compiler, VariableExpression ve) {
            super(exp, right.getType());
            this.right = right;
            this.compiler = compiler;
            this.ve = ve;
        }

        protected void compile(MethodVisitor mv) {
            right.visit(mv);
            dup(right.getType(), mv);
            compiler.compileStack.defineTypeInferenceVariable(ve, right.getType());
        }
    }
}