package org.mbte.groovypp.compiler.bytecode;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.expr.TupleExpression;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.mbte.groovypp.compiler.*;
import org.mbte.groovypp.compiler.transformers.VariableExpressionTransformer;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.List;

public class ResolvedMethodBytecodeExpr extends BytecodeExpr {
    private final MethodNode methodNode;
    private final BytecodeExpr object;
    private final String methodName;

    private final TupleExpression bargs;

    private ResolvedMethodBytecodeExpr(ASTNode parent, MethodNode methodNode, BytecodeExpr object, TupleExpression bargs, CompilerTransformer compiler) {
        super(parent, getReturnType(methodNode, object, bargs, compiler));
        this.methodNode = methodNode;
        this.object = object;
        this.methodName = methodNode.getName();
        this.bargs = bargs;

        tryImproveClosureType(methodNode, bargs);

        Parameter[] parameters = methodNode.getParameters();
        boolean isVarArg = parameters.length > 0 && parameters[parameters.length - 1].getType().isArray();

        if (isVarArg) {
            if (bargs.getExpressions().size() == parameters.length - 1) {
                // no var args
                BytecodeExpr last = (BytecodeExpr) compiler.transform(new ArrayExpression(parameters[parameters.length - 1].getType().getComponentType(), new ArrayList<Expression>()));
                bargs.getExpressions().add(last);
            } else {
                BytecodeExpr be = (BytecodeExpr) bargs.getExpressions().get(parameters.length - 1);
                if (!be.getType().isArray() && !be.getType().equals(parameters[parameters.length - 1].getType())) {
                    int add = bargs.getExpressions().size() - (parameters.length - 1);
                    ArrayList<Expression> list = new ArrayList<Expression>();

                    if (add != 1 || !bargs.getExpressions().get(parameters.length - 1).getType().equals(TypeUtil.NULL_TYPE)) {
                        for (int i = 0; i != add; ++i) {
                            final BytecodeExpr arg = (BytecodeExpr) bargs.getExpressions().get(parameters.length - 1 + i);

                            if (parameters[parameters.length - 1].getType().getComponentType().equals(ClassHelper.STRING_TYPE)) {
                                if (!arg.getType().equals(ClassHelper.STRING_TYPE)) {
                                    list.add(new BytecodeExpr(arg, ClassHelper.STRING_TYPE) {
                                        protected void compile(MethodVisitor mv) {
                                            arg.visit(mv);
                                            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "toString", "()Ljava/lang/String;");
                                        }
                                    });
                                } else
                                    list.add(arg);
                            } else
                                list.add(arg);
                        }

                        while (bargs.getExpressions().size() > parameters.length - 1)
                            bargs.getExpressions().remove(parameters.length - 1);

                        BytecodeExpr last = (BytecodeExpr) compiler.transform(new ArrayExpression(parameters[parameters.length - 1].getType().getComponentType(), list));
                        bargs.getExpressions().add(last);
                    }
                }
            }
        }

        while (bargs.getExpressions().size() < parameters.length)
            bargs.getExpressions().add(compiler.cast(ConstantExpression.NULL, parameters[bargs.getExpressions().size()].getType()));

        for (int i = 0; i != parameters.length; ++i) {
            final BytecodeExpr arg = (BytecodeExpr) bargs.getExpressions().get(i);
            ClassNode ptype = parameters[i].getType();

            if (!ptype.equals(arg.getType()))
                bargs.getExpressions().set(i, compiler.cast(arg, ptype));

//            if (ptype.equals(ClassHelper.STRING_TYPE)) {
//                if (!arg.getType().equals(ClassHelper.STRING_TYPE)) {
//                    bargs.getExpressions().set(i, new BytecodeExpr(arg, ptype) {
//                        protected void compile(MethodVisitor mv) {
//                            arg.visit(mv);
//                            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "toString", "()Ljava/lang/String;");
//                        }
//                    });
//                }
//            } else if (ptype.isArray()) {
//                final ClassNode componentType = ptype.getComponentType();
//                if (ClassHelper.isPrimitiveType(componentType)) {
//                    bargs.getExpressions().set(i, new BytecodeExpr(arg, ptype) {
//                        protected void compile(MethodVisitor mv) {
//                            arg.visit(mv);
//                            if (componentType == ClassHelper.byte_TYPE)
//                                mv.visitMethodInsn(INVOKESTATIC, "org/codehaus/groovy/runtime/typehandling/DefaultTypeTransformation", "convertToByteArray", "(Ljava/lang/Object;)[B");
//                            else if (componentType == ClassHelper.boolean_TYPE)
//                                mv.visitMethodInsn(INVOKESTATIC, "org/codehaus/groovy/runtime/typehandling/DefaultTypeTransformation", "convertToBooleanArray", "(Ljava/lang/Object;)[Z");
//                            else if (componentType == ClassHelper.short_TYPE)
//                                mv.visitMethodInsn(INVOKESTATIC, "org/codehaus/groovy/runtime/typehandling/DefaultTypeTransformation", "convertToShortArray", "(Ljava/lang/Object;)[S");
//                            else if (componentType == ClassHelper.int_TYPE)
//                                mv.visitMethodInsn(INVOKESTATIC, "org/codehaus/groovy/runtime/typehandling/DefaultTypeTransformation", "convertToIntArray", "(Ljava/lang/Object;)[I");
//                            else if (componentType == ClassHelper.char_TYPE)
//                                mv.visitMethodInsn(INVOKESTATIC, "org/codehaus/groovy/runtime/typehandling/DefaultTypeTransformation", "convertToCharArray", "(Ljava/lang/Object;)[C");
//                            else if (componentType == ClassHelper.long_TYPE)
//                                mv.visitMethodInsn(INVOKESTATIC, "org/codehaus/groovy/runtime/typehandling/DefaultTypeTransformation", "convertToLongArray", "(Ljava/lang/Object;)[L");
//                            else if (componentType == ClassHelper.float_TYPE)
//                                mv.visitMethodInsn(INVOKESTATIC, "org/codehaus/groovy/runtime/typehandling/DefaultTypeTransformation", "convertToFloatArray", "(Ljava/lang/Object;)[F");
//                            else if (componentType == ClassHelper.double_TYPE)
//                                mv.visitMethodInsn(INVOKESTATIC, "org/codehaus/groovy/runtime/typehandling/DefaultTypeTransformation", "convertToDoubleArray", "(Ljava/lang/Object;)[D");
//                        }
//                    });
//                } else if (componentType.equals(ClassHelper.OBJECT_TYPE) && ClassHelper.isPrimitiveType(arg.getType().getComponentType())) {
//                    bargs.getExpressions().set(i, new BytecodeExpr(arg, ptype) {
//                        protected void compile(MethodVisitor mv) {
//                            arg.visit(mv);
//                            mv.visitMethodInsn(INVOKESTATIC, "org/codehaus/groovy/runtime/typehandling/DefaultTypeTransformation", "primitiveArrayBox", "(Ljava/lang/Object;)[Ljava/lang/Object;");
//                        }
//                    });
//                } else if (componentType.equals(ClassHelper.STRING_TYPE) && arg.getType().getComponentType().equals(ClassHelper.GSTRING_TYPE)) {
//                    bargs.getExpressions().set(i, new BytecodeExpr(arg, ptype) {
//                        protected void compile(MethodVisitor mv) {
//                            arg.visit(mv);
//                            mv.visitMethodInsn(INVOKESTATIC, "org/mbte/groovypp/runtime/DefaultGroovyPPMethods", "gstringArrayToStringArray", "([Lgroovy/lang/GString;)[Ljava/lang/String;");
//                        }
//                    });
//                }
//            }
        }
    }

    public static ClassNode getReturnType(MethodNode methodNode, BytecodeExpr object, TupleExpression bargs, CompilerTransformer compiler) {
        final BytecodeExpr objectCopy = object;
        ClassNode returnType = methodNode.getReturnType();
        
        boolean removeFirstArgAtTheEnd = false;
        if (methodNode instanceof ClassNodeCache.DGM) {
            ClassNodeCache.DGM dgm = (ClassNodeCache.DGM) methodNode;
            methodNode = dgm.original;
            bargs.getExpressions().add(0, object);
            object = null;
            removeFirstArgAtTheEnd = true;
        }

        if (TypeUtil.hasGenericsTypes(methodNode)) {
            Parameter[] params = methodNode.getParameters();
            List<Expression> exprs = bargs.getExpressions();
            int length;
            if (exprs.size() > params.length && params[params.length - 1].getType().isArray()) {
                length = exprs.size();
            } else {
                length = Math.min(params.length, exprs.size());
            }

            if (!methodNode.isStatic())
                length++;

            ClassNode[] paramTypes = new ClassNode[length];
            ClassNode[] argTypes   = new ClassNode[length];

            int delta = 0;
            if (!methodNode.isStatic()) {
                paramTypes [length-1] = methodNode.getDeclaringClass();
                argTypes [length-1] = object != null ? object.getType() : null;
                delta = 1;
            }
            for (int i = 0; i < length-delta; i++) {
                paramTypes[i] = i > params.length + delta - 1 ||
                            (i == params.length - 1 && params[i].getType().isArray() && !bargs.getExpression(i).getType().isArray()) ?
                        /* varargs case */ params[params.length - 1].getType().getComponentType() :
                        params[i].getType();
                argTypes[i] = bargs.getExpression(i).getType();
            }
            ClassNode[] bindings = TypeUnification.inferTypeArguments(methodNode.getGenericsTypes(), paramTypes, argTypes);
            returnType = TypeUtil.getSubstitutedType(returnType, methodNode, bindings);

            for (int i = 0; i < length-delta; i++) {
                ClassNode paramType = TypeUtil.getSubstitutedType(paramTypes[i], methodNode, bindings);
                if (objectCopy != null) {
                    paramType = TypeUtil.getSubstitutedType(paramType, methodNode.getDeclaringClass(),
                            objectCopy.getType());
                }
                bargs.getExpressions().set(i, compiler.cast(bargs.getExpressions().get(i), paramType));
            }

        }

        if (removeFirstArgAtTheEnd) {
            bargs.getExpressions().remove(0);
        }

        return objectCopy != null ? TypeUtil.getSubstitutedType(returnType, methodNode.getDeclaringClass(), objectCopy.getType()) : returnType;
    }

    private void tryImproveClosureType(MethodNode methodNode, TupleExpression bargs) {
        final Parameter[] parameters = methodNode.getParameters();
        if (parameters.length > 0) {
            final Parameter lastParam = parameters[parameters.length - 1];
            if (lastParam.getType().getName().equals(TypeUtil.TCLOSURE.getName())) {
                if (lastParam.getType().isUsingGenerics()) {
                    final GenericsType[] genericsTypes = lastParam.getType().getGenericsTypes();
                    if (genericsTypes != null) {
                        Object param = bargs.getExpressions().get(parameters.length - 1);
                        if (param instanceof CompiledClosureBytecodeExpr) {
                            CompiledClosureBytecodeExpr ce = (CompiledClosureBytecodeExpr) param;
                            ce.getType().getInterfaces()[0].setGenericsTypes(new GenericsType[]{new GenericsType(genericsTypes[0].getType())});
                        }
                    }
                }
            }
        }
    }

    public void compile(MethodVisitor mv) {
        int op = INVOKEVIRTUAL;
        final String classInternalName;
        final String methodDescriptor;
        if (methodNode instanceof ClassNodeCache.DGM) {
            MethodNode dgm = ((ClassNodeCache.DGM) methodNode).original;

            op = INVOKESTATIC;

            if (object != null) {
                object.visit(mv);
                if (ClassHelper.isPrimitiveType(object.getType()) && !ClassHelper.isPrimitiveType(dgm.getParameters()[0].getType()))
                    box(object.getType(), mv);
            } else if (methodNode.isStatic()) {
                // DGSM method needs fake argument.
                mv.visitInsn(ACONST_NULL);
            }

            classInternalName = ((ClassNodeCache.DGM) methodNode).callClassInternalName;
            methodDescriptor = ((ClassNodeCache.DGM) methodNode).descr;
        } else {
            if (methodNode.isStatic())
                op = INVOKESTATIC;
            else if (methodNode.getDeclaringClass().isInterface())
                op = INVOKEINTERFACE;

            if (object != null) {
                if (object instanceof VariableExpressionTransformer.Super || object instanceof VariableExpressionTransformer.ThisSpecial) {
                    op = INVOKESPECIAL;
                }

                object.visit(mv);
                box(object.getType(), mv);
            }

            if (op == INVOKESTATIC && object != null) {
                if (ClassHelper.long_TYPE == object.getType() || ClassHelper.double_TYPE == object.getType())
                    mv.visitInsn(POP2);
                else
                    mv.visitInsn(POP);
            }
            
            ClassNode t = op == INVOKESPECIAL ? object.getType() : methodNode.getDeclaringClass();
            classInternalName = (t.isArray()) ? BytecodeHelper.getTypeDescription(t) : BytecodeHelper.getClassInternalName(t); 
            methodDescriptor = BytecodeHelper.getMethodDescriptor(methodNode.getReturnType(), methodNode.getParameters());
        }

        loadParams(mv, op == INVOKESTATIC);
        mv.visitMethodInsn(op, classInternalName, methodName, methodDescriptor);

        if (!methodNode.getReturnType().equals(ClassHelper.VOID_TYPE))
            cast(TypeUtil.wrapSafely(methodNode.getReturnType()), TypeUtil.wrapSafely(getType()), mv);
    }

    protected void loadParams(MethodVisitor mv, boolean isStstic) {
        Parameter[] parameters = methodNode.getParameters();
        for (int i = 0; i != parameters.length; ++i) {
            BytecodeExpr be = (BytecodeExpr) bargs.getExpressions().get(i);
            be.visit(mv);
            final ClassNode paramType = parameters[i].getType();
            final ClassNode type = be.getType();
            box(type, mv);
            unbox(paramType, mv);
        }
    }

    public MethodNode getMethodNode() {
        return methodNode;
    }

    public TupleExpression getBargs() {
        return bargs;
    }

    public BytecodeExpr getObject() {
        return object;
    }

    public static class Setter extends ResolvedMethodBytecodeExpr {
        public Setter(ASTNode parent, MethodNode methodNode, BytecodeExpr object, ArgumentListExpression bargs, CompilerTransformer compiler) {
            super(parent, methodNode, object, bargs, compiler);
            setType(bargs.getExpressions().get(0).getType());
        }

        @Override
        protected void loadParams(MethodVisitor mv, boolean isStatic) {
            super.loadParams(mv, isStatic);
            if (isStatic)
                dup(getType(), mv);
            else
                dup_x1(getType(), mv);
        }
    }

    public static ResolvedMethodBytecodeExpr create(ASTNode parent, MethodNode methodNode, BytecodeExpr object, TupleExpression bargs, CompilerTransformer compiler) {
        if ((methodNode.getModifiers() & Opcodes.ACC_PRIVATE) != 0 && methodNode.getDeclaringClass() != compiler.classNode) {
            MethodNode delegate = compiler.context.getMethodDelegate(methodNode);
            return new ResolvedMethodBytecodeExpr(parent, delegate, object, bargs, compiler);
        }
        return new ResolvedMethodBytecodeExpr(parent, methodNode, object, bargs, compiler);
    }
}
