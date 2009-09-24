package org.mbte.groovypp.compiler.transformers;

import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Label;

import java.util.IdentityHashMap;

public abstract class ExprTransformer<T extends Expression> implements Opcodes {

    private static IdentityHashMap<Class,ExprTransformer> transformers = new IdentityHashMap<Class,ExprTransformer> ();

    static {
        transformers.put(CastExpression.class, new CastExpressionTransformer());
        transformers.put(ClassExpression.class, new ClassExpressionTransformer());
        transformers.put(ConstantExpression.class, new ConstantExpressionTransformer());
        transformers.put(ListExpression.class, new ListExpressionTransformer());
        transformers.put(MapExpression.class, new MapExpressionTransformer());
        transformers.put(SpreadExpression.class, new SpreadExpressionTransformer());
        transformers.put(VariableExpression.class, new VariableExpressionTransformer());
        transformers.put(DeclarationExpression.class, new DeclarationExpressionTransformer());
        transformers.put(ClassExpression.class, new ClassExpressionTransformer());
        transformers.put(ClosureExpression.class, new ClosureExpressionTransformer());
        transformers.put(MethodCallExpression.class, new MethodCallExpressionTransformer());
        transformers.put(PostfixExpression.class, new PostfixExpressionTransformer());
        transformers.put(PrefixExpression.class, new PrefixExpressionTransformer());
        transformers.put(PropertyExpression.class, new PropertyExpressionTransformer());
        transformers.put(BinaryExpression.class, new BinaryExpressionTransformer());
        transformers.put(GStringExpression.class, new GStringExpressionTransformer());
        transformers.put(ConstructorCallExpression.class, new ConstructorCallExpressionTransformer());
        transformers.put(RangeExpression.class, new RangeExpressionTransformer());
        transformers.put(FieldExpression.class, new FieldExpressionTransformer());
        transformers.put(UnaryMinusExpression.class, new UnaryMinusExpressionTransformer());
        transformers.put(UnaryPlusExpression.class, new UnaryPlusExpressionTransformer());

        final BooleanExpressionTransformer bool = new BooleanExpressionTransformer();
        transformers.put(BooleanExpression.class, bool);
        transformers.put(NotExpression.class, bool);

        final TernaryExpressionTransformer ternary = new TernaryExpressionTransformer();
        transformers.put(TernaryExpression.class, ternary);
        transformers.put(ElvisOperatorExpression.class, ternary);
    }

    public static Expression transformExpression (Expression exp, CompilerTransformer compiler) {
        ExprTransformer t = transformers.get(exp.getClass());
        if (t == null)
            return compiler.transformImpl(exp);

        return t.transform(exp, compiler);
    }

    public static BytecodeExpr transformLogicalExpression(Expression exp, CompilerTransformer compiler, Label label, boolean onTrue) {
        ExprTransformer t = transformers.get(exp.getClass());
        return t.transformLogical(exp, compiler, label, onTrue);
    }

    public abstract Expression transform (T exp, CompilerTransformer compiler);

    public BytecodeExpr transformLogical (T exp, CompilerTransformer compiler, final Label label, final boolean onTrue) {
        final BytecodeExpr be = (BytecodeExpr) transform(exp, compiler);
        final ClassNode type = be.getType();

        if (type == ClassHelper.VOID_TYPE) {
            return be;
        }
        return new BytecodeExpr(exp, ClassHelper.VOID_TYPE) {
            protected void compile() {
                be.visit(mv);

                if (ClassHelper.isPrimitiveType(type)) {
                    if (type == ClassHelper.byte_TYPE
                     || type == ClassHelper.short_TYPE
                     || type == ClassHelper.char_TYPE
                     || type == ClassHelper.boolean_TYPE
                     || type == ClassHelper.int_TYPE) {
                    } else if (type == ClassHelper.long_TYPE) {
                        mv.visitInsn(L2I);
                    } else if (type == ClassHelper.float_TYPE) {
                        mv.visitInsn(F2I);
                    } else if (type == ClassHelper.double_TYPE) {
                        mv.visitInsn(D2I);
                    }
                }
                else {
                    mv.visitMethodInsn(INVOKESTATIC, DTT, "castToBoolean", "(Ljava/lang/Object;)Z");
                }
                mv.visitJumpInsn(onTrue ? IFNE : IFEQ, label);
            }
        };
    }

    private static final String DTT = BytecodeHelper.getClassInternalName(DefaultTypeTransformation.class.getName());
}
