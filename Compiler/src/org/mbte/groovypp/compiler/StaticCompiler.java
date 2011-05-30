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

package org.mbte.groovypp.compiler;

import groovy.lang.EmptyRange;
import groovy.lang.TypePolicy;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.*;
import org.codehaus.groovy.classgen.BytecodeInstruction;
import org.codehaus.groovy.classgen.BytecodeSequence;
import org.codehaus.groovy.control.Janitor;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;
import org.codehaus.groovy.util.FastArray;
import org.mbte.groovypp.compiler.bytecode.*;
import org.mbte.groovypp.compiler.flow.*;
import org.mbte.groovypp.runtime.powerassert.SourceText;
import org.mbte.groovypp.runtime.powerassert.SourceTextNotAvailableException;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.codehaus.groovy.ast.ClassHelper.*;

public class StaticCompiler extends CompilerTransformer implements Opcodes {
    private StaticMethodBytecode methodBytecode;

    // exception blocks list
    private List<Runnable> exceptionBlocks = new ArrayList<Runnable>();

    final boolean shouldImproveReturnType;

    ClassNode calculatedReturnType = TypeUtil.NULL_TYPE;
    private Label startLabel = new Label ();

    public StaticCompiler(SourceUnit su, SourceUnitContext context, StaticMethodBytecode methodBytecode, StackAwareMethodAdapter mv, org.mbte.groovypp.compiler.CompilerStack compileStack, int debug, boolean fastArrays, TypePolicy policy, String baseClosureName) {
        super(su, methodBytecode.methodNode.getDeclaringClass(), methodBytecode.methodNode, mv, compileStack, debug, fastArrays, policy, baseClosureName, context);
        this.methodBytecode = methodBytecode;
        shouldImproveReturnType = methodNode.getName().equals("doCall") || methodNode.getReturnType() == TypeUtil.IMPROVE_TYPE;

        mv.visitLabel(startLabel);
        if (methodNode instanceof ConstructorNode && !((ConstructorNode) methodNode).firstStatementIsSpecialConstructorCall()) {
            // invokes the super class constructor
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, BytecodeHelper.getClassInternalName(classNode.getSuperClass()), "<init>", "()V");
        }
    }

    public static void closureToMethod(ClassNode type, CompilerTransformer compiler, ClassNode objType, String keyName, ClosureExpression ce) {
        if (ce.getParameters() != null && ce.getParameters().length == 0) {
            final VariableScope scope = ce.getVariableScope();
            ce = new ClosureExpression(new Parameter[1], ce.getCode());
            ce.setVariableScope(scope);
            ce.getParameters()[0] = new Parameter(OBJECT_TYPE, "it", new ConstantExpression(null));
        }

        final ClosureMethodNode _doCallMethod = new ClosureMethodNode(
                keyName,
                Opcodes.ACC_PUBLIC|Opcodes.ACC_FINAL,
                TypeUtil.IMPROVE_TYPE,
                ce.getParameters() == null ? Parameter.EMPTY_ARRAY : ce.getParameters(),
                ce.getCode());

        if(objType != type)
            objType.addMethod(_doCallMethod);

        _doCallMethod.createDependentMethods(objType);

        Object methods = ClassNodeCache.getMethods(type, keyName);
        if (methods != null) {
            if (methods instanceof MethodNode) {
                MethodNode baseMethod = (MethodNode) methods;
                _doCallMethod.checkOverride(baseMethod, type);
            }
            else {
                FastArray methodsArr = (FastArray) methods;
                int methodCount = methodsArr.size();
                for (int j = 0; j != methodCount; ++j) {
                    MethodNode baseMethod = (MethodNode) methodsArr.get(j);
                    _doCallMethod.checkOverride(baseMethod, type);
                }
            }
        }

        if(objType == type)
            objType.addMethod(_doCallMethod);

        ClassNodeCache.clearCache (_doCallMethod.getDeclaringClass());
        compiler.replaceMethodCode(_doCallMethod.getDeclaringClass(), _doCallMethod);
    }

    protected Statement getCode() {
        return methodBytecode.code;
    }

    protected void setCode(Statement statement) {
        methodBytecode.code = statement;
    }

    protected SourceUnit getSourceUnit() {
        return methodBytecode.su;
    }

    private int lastLine = -1;

    @Override
    protected void visitStatement(Statement statement) {
        super.visitStatement(statement);

        int line = statement.getLineNumber();
        if (line >= 0 && mv != null && line != lastLine) {
            Label l = new Label();
            mv.visitLabel(l);
            mv.visitLineNumber(line, l);
            lastLine = line;
        }
    }

    private static class AssertionTracker {
        int recorderIndex;
        SourceText sourceText;
    }

    private AssertionTracker assertionTracker;

    @Override
    public void visitAssertStatement(AssertStatement statement) {
        boolean rewriteAssert = true;
        // don't rewrite assertions with message
        rewriteAssert = statement.getMessageExpression() == ConstantExpression.NULL;

        if(rewriteAssert) {
            Janitor janitor = new Janitor();
            try {
                SourceText sourceText = null;
                try {
                    sourceText = new SourceText(statement, su, janitor);
                }
                catch (SourceTextNotAvailableException e) {
                    rewriteAssert = false;
                }

                AssertionTracker oldTracker = assertionTracker;
                if(rewriteAssert) {
                    assertionTracker = new AssertionTracker();
                    try {

                        final BlockStatement block = new BlockStatement();

                        final VariableExpression variable = new VariableExpression("__recorder", TypeUtil.VALUE_RECORDER);
                        variable.setSourcePosition(statement);

                        final ConstructorCallExpression newRecorder = new ConstructorCallExpression(TypeUtil.VALUE_RECORDER, new ArgumentListExpression());
                        newRecorder.setSourcePosition(statement);

                        final DeclarationExpression declaration = new DeclarationExpression(variable, Token.newSymbol(Types.EQUAL, -1, -1), newRecorder);
                        declaration.setSourcePosition(statement);

                        final ExpressionStatement declarationStatement = new ExpressionStatement(declaration);
                        declarationStatement.setSourcePosition(statement);

                        block.addStatement(declarationStatement);

                        final BlockStatement tryStatement = new BlockStatement();
                        tryStatement.setSourcePosition(statement);

                        MethodCallExpression render = new MethodCallExpression(new ClassExpression(TypeUtil.ASSERTION_RENDERER), "render", new ArgumentListExpression(new ConstantExpression(sourceText.getNormalizedText()), variable));
                        render.setSourcePosition(statement);

                        MethodCallExpression assertFailed = new MethodCallExpression(new ClassExpression(TypeUtil.SCRIPT_BYTECODE_ADAPTER), "assertFailed", new ArgumentListExpression(render, ConstantExpression.NULL));
                        assertFailed.setSourcePosition(statement);

                        final ThrowStatement elseStatement = new ThrowStatement(new ConstructorCallExpression(TypeUtil.POWER_ASSERT_ERROR, new ArgumentListExpression(render)));
                        elseStatement.setSourcePosition(statement);


                        final SourceText finalSourceText = sourceText;
                        final ClassCodeExpressionTransformer transformer = new ClassCodeExpressionTransformer(){

                            @Override
                            protected SourceUnit getSourceUnit() {
                                return su;
                            }

                            @Override
                            public Expression transform(Expression exp) {
                                if(exp instanceof AndExpression) {
                                    AndExpression endExpr = (AndExpression) exp;
                                    final Token token = endExpr.getOperation();
                                    int column = finalSourceText.getNormalizedColumn(token.getStartLine(), token.getStartColumn());
                                    final MethodCallExpression res = new MethodCallExpression(variable, "gppRecord", new ArgumentListExpression(super.transform(exp), new ConstantExpression(column)));
                                    res.setSourcePosition(exp);
                                    return res;
                                }

                                if(exp instanceof OrExpression) {
                                    OrExpression endExpr = (OrExpression) exp;
                                    final Token token = endExpr.getOperation();
                                    int column = finalSourceText.getNormalizedColumn(token.getStartLine(), token.getStartColumn());
                                    final MethodCallExpression res = new MethodCallExpression(variable, "gppRecord", new ArgumentListExpression(super.transform(exp), new ConstantExpression(column)));
                                    res.setSourcePosition(exp);
                                    return res;
                                }

                                if(exp instanceof BinaryExpression) {
                                    BinaryExpression binExpr = (BinaryExpression) exp;
                                    final Token token = binExpr.getOperation();
                                    if (token.getType() != Types.LEFT_SQUARE_BRACKET) {
                                        int column = finalSourceText.getNormalizedColumn(token.getStartLine(), token.getStartColumn());
                                        final MethodCallExpression res = new MethodCallExpression(variable, "gppRecord", new ArgumentListExpression(super.transform(exp), new ConstantExpression(column)));
                                        res.setSourcePosition(exp);
                                        return res;
                                    }
                                    else {
                                        return super.transform(exp);
                                    }
                                }

                                if(exp instanceof VariableExpression) {
                                    int column = finalSourceText.getNormalizedColumn(exp.getLineNumber(), exp.getColumnNumber());
                                    return new RecordingVariableExpression((VariableExpression) exp, variable, column);
                                }

                                if(exp instanceof MethodCallExpression) {
                                    final Expression method = ((MethodCallExpression) exp).getMethod();
                                    int column = finalSourceText.getNormalizedColumn(method.getLineNumber(), method.getColumnNumber());
                                    final MethodCallExpression res = new MethodCallExpression(variable, "gppRecord", new ArgumentListExpression(super.transform(exp), new ConstantExpression(column)));
                                    res.setSourcePosition(exp);
                                    return res;
                                }

                                if(exp instanceof PropertyExpression) {
                                    Expression property = ((PropertyExpression) exp).getProperty();
                                    int column = finalSourceText.getNormalizedColumn(property.getLineNumber(), property.getColumnNumber());
                                    final MethodCallExpression res = new MethodCallExpression(variable, "gppRecord", new ArgumentListExpression(super.transform(exp), new ConstantExpression(column)));
                                    res.setSourcePosition(exp);
                                    return res;
                                }

                                if(exp instanceof BooleanExpression) {
                                    int column = finalSourceText.getNormalizedColumn(exp.getLineNumber(), exp.getColumnNumber());
                                    Expression subExpr = super.transform(exp);

                                    final MethodCallExpression res = new MethodCallExpression(variable, "gppRecord", new ArgumentListExpression(subExpr, new ConstantExpression(column)));
                                    res.setSourcePosition(exp);
                                    return res;
                                }

                                if(exp instanceof PostfixExpression || exp instanceof PrefixExpression) {
                                    return exp;
                                }

                                return super.transform(exp);
                            }
                        };

                        final BooleanExpression transformed = new BooleanExpression(transformer.transform(statement.getBooleanExpression().getExpression()));
                        transformed.setSourcePosition(statement.getBooleanExpression().getExpression());

                        final IfStatement ifStatement = new IfStatement(transformed, EmptyStatement.INSTANCE, elseStatement);
                        ifStatement.setSourcePosition(statement);

                        tryStatement.addStatement(ifStatement);

                        final BlockStatement finallyStatement = new BlockStatement();
                        finallyStatement.setSourcePosition(statement);

                        MethodCallExpression clearExpression = new MethodCallExpression(variable, "clear", new ArgumentListExpression());
                        clearExpression.setSourcePosition(statement);

                        final ExpressionStatement clearStatement = new ExpressionStatement(clearExpression);
                        clearStatement.setSourcePosition(statement);

                        finallyStatement.addStatement(clearStatement);

                        TryCatchStatement tryCatch = new TryCatchStatement(tryStatement, finallyStatement);
                        tryCatch.setSourcePosition(statement);

                        block.addStatement(tryCatch);

                        block.visit(this);
                    }
                    finally {
                        assertionTracker = oldTracker;
                    }
                }
            }
            finally {
                janitor.cleanup();
            }

            return;
        }

        visitStatement(statement);
        Label noError = new Label();

        BytecodeExpr condition = transformLogical(statement.getBooleanExpression().getExpression(), noError, true);

        Expression msg = statement.getMessageExpression();
        if (msg instanceof ConstantExpression && ((ConstantExpression)msg).getValue() == null)
            msg = new ConstantExpression("\n" + su.getSample(statement.getLineNumber(), 0, null));
        BytecodeExpr msgExpr = (BytecodeExpr) transform(msg);

        condition.visit(mv);
        mv.visitTypeInsn(NEW, "java/lang/AssertionError");
        mv.visitInsn(DUP);
        if (msgExpr != null)
            msgExpr.visit(mv);
        else
            mv.visitLdcInsn("<no message>");
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/AssertionError", "<init>", "(Ljava/lang/Object;)V");
        mv.visitInsn(ATHROW);
        mv.visitLabel(noError);
    }

    public static final String DTT = BytecodeHelper.getClassInternalName(DefaultTypeTransformation.class.getName());

    public void visitBlockStatement(BlockStatement statement) {
        if(statement.getStatementLabel() != null) {
            ClosureExpression closureExpression = new ClosureExpression(null, statement);
            closureExpression.setSourcePosition(statement);
            closureToMethod(classNode, this, classNode, statement.getStatementLabel(), closureExpression);
            return;
        }

        compileStack.pushVariableScope(statement.getVariableScope());
        for (Statement st : statement.getStatements() ) {
            if (st instanceof BytecodeSequence)
                visitBytecodeSequence((BytecodeSequence) st);
            else {
                if(st instanceof org.mbte.groovypp.compiler.flow.LabelStatement) {
                    mv.visitLabel(((org.mbte.groovypp.compiler.flow.LabelStatement)st).labelExpression.label);
                }
                else
                    st.visit(this);
            }
        }
        compileStack.pop();
    }

    public void visitBreakStatement(BreakStatement statement) {
        visitStatement(statement);

        String name = statement.getLabel();
        Label breakLabel = compileStack.getNamedBreakLabel(name);
        if (breakLabel == null) {
            addError("Illegal break label '" + name + "'", statement);
        }

        compileStack.applyFinallyBlocks(breakLabel, true);

        mv.visitJumpInsn(GOTO, breakLabel);
    }

    public void visitExpressionStatement(ExpressionStatement statement) {
        if(statement.getStatementLabel() != null) {
            if(statement.getExpression() instanceof ClosureExpression) {
                closureToMethod(classNode, this, classNode, statement.getStatementLabel(), (ClosureExpression)statement.getExpression());
                return;
            }
        }

        visitStatement(statement);

        super.visitExpressionStatement(statement);

        final BytecodeExpr be = transformSynthetic((BytecodeExpr) statement.getExpression());
        be.visit(mv);
        final ClassNode type = be.getType();
        if (type != ClassHelper.VOID_TYPE && type != ClassHelper.void_WRAPPER_TYPE) {
            BytecodeExpr.pop(type, mv);
        }
    }

    private void visitForLoopWithIterator(ForStatement forLoop, BytecodeExpr collectionExpression) {
        compileStack.pushLoop(forLoop.getVariableScope(), forLoop.getStatementLabel());

        Label continueLabel = compileStack.getContinueLabel();
        Label breakLabel = compileStack.getBreakLabel();
        BytecodeExpr fakeObject = new BytecodeExpr(collectionExpression, collectionExpression.getType()) {
            protected void compile(MethodVisitor mv) {}
        };

        MethodCallExpression iterator = new MethodCallExpression(
                fakeObject, "iterator", new ArgumentListExpression());
        iterator.setSourcePosition(collectionExpression);
        BytecodeExpr expr = (BytecodeExpr) transform(iterator);
        expr.visit(mv);

        ClassNode etype =  ClassHelper.OBJECT_TYPE;
        ClassNode iteratorType = expr.getType();
        GenericsType[] generics = iteratorType.getGenericsTypes();
        if (generics != null && generics.length == 1) {
            if (!TypeUtil.isSuper(generics[0])) {
                etype = generics[0].getType();
            }
        }
        if (forLoop.getVariable().getType() == ClassHelper.DYNAMIC_TYPE)
            forLoop.getVariable().setType(etype);
        else
            etype = forLoop.getVariable().getType();

        Register variable = compileStack.defineVariable(forLoop.getVariable(), false);

        final int iteratorIdx = compileStack.defineTemporaryVariable(
                "iterator", ClassHelper.make(Iterator.class), true);

        mv.startLoopVisitLabel(continueLabel);
        mv.visitVarInsn(ALOAD, iteratorIdx);
        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "hasNext", "()Z");
        mv.visitJumpInsn(IFEQ, breakLabel);

        mv.visitVarInsn(ALOAD, iteratorIdx);
        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "next", "()Ljava/lang/Object;");
        if (ClassHelper.isPrimitiveType(etype)) {
            BytecodeExpr.unbox(etype, mv);
        } else {
            BytecodeExpr.cast(ClassHelper.OBJECT_TYPE, etype, mv);
        }
        BytecodeExpr.store(etype, variable.getIndex(), mv);

        forLoop.getLoopBlock().visit(this);

        mv.visitJumpInsn(GOTO, continueLabel);
        mv.visitLabel(breakLabel);
        compileStack.pop();
    }

    private void visitForLoopWithClosures(ForStatement forLoop) {

        compileStack.pushLoop(forLoop.getVariableScope(), forLoop.getStatementLabel());

        ClosureListExpression closureExpression = (ClosureListExpression) forLoop.getCollectionExpression();
        compileStack.pushVariableScope(closureExpression.getVariableScope());

        Label continueLabel = compileStack.getContinueLabel();
        Label breakLabel = compileStack.getBreakLabel();
        List<Expression> loopExpr = closureExpression.getExpressions();

        if (!(loopExpr.get(0) instanceof EmptyExpression)) {
            final BytecodeExpr initExpression = (BytecodeExpr) transform(loopExpr.get(0));
            initExpression.visit(mv);
            BytecodeExpr.pop(initExpression.getType(), mv);
        }

        Label cond = new Label();
        mv.startLoopVisitLabel(cond);

        if (!(loopExpr.get(1) instanceof EmptyExpression)) {
            final BytecodeExpr binaryExpression = transformLogical(loopExpr.get(1), breakLabel, false);
            binaryExpression.visit(mv);
        }

        forLoop.getLoopBlock().visit(this);

        mv.visitLabel(continueLabel);

        if (!(loopExpr.get(2) instanceof EmptyExpression)) {
            final BytecodeExpr incrementExpression = (BytecodeExpr) transform(loopExpr.get(2));

            incrementExpression.visit(mv);
            final ClassNode type = incrementExpression.getType();
            if (type != ClassHelper.VOID_TYPE && type != ClassHelper.void_WRAPPER_TYPE) {
                if (type == ClassHelper.long_TYPE || type == ClassHelper.double_TYPE) {
                    mv.visitInsn(POP2);
                } else {
                    mv.visitInsn(POP);
                }
            }
        }

        mv.visitJumpInsn(GOTO, cond);
        mv.visitLabel(breakLabel);

        compileStack.pop();
        compileStack.pop();
    }

    @Override
    public void visitForLoop(ForStatement forLoop) {
        visitStatement(forLoop);
        Parameter loopVar = forLoop.getVariable();
        if (loopVar == ForStatement.FOR_LOOP_DUMMY) {
            visitForLoopWithClosures(forLoop);
        } else {
            BytecodeExpr collectionExpression = (BytecodeExpr) transformToGround(forLoop.getCollectionExpression());
            collectionExpression.visit(mv);
            Label nullLabel = new Label(), endLabel = new Label();
            mv.visitInsn(DUP);
            mv.visitJumpInsn(IFNULL, nullLabel);
            if (collectionExpression.getType().isArray()) {
                visitForLoopWithArray(forLoop, collectionExpression.getType().getComponentType());
            } else if (forLoop.getCollectionExpression() instanceof RangeExpression &&
                    TypeUtil.equal(TypeUtil.RANGE_OF_INTEGERS_TYPE, collectionExpression.getType())
                    && (forLoop.getVariable().getType() == ClassHelper.DYNAMIC_TYPE ||
                        forLoop.getVariable().getType().equals(ClassHelper.int_TYPE))) {
                // This is the IntRange (or EmptyRange). Iterate with index.
                visitForLoopWithIntRange(forLoop);
            } else {
                visitForLoopWithIterator(forLoop, collectionExpression);
            }
            mv.visitJumpInsn(GOTO, endLabel);
            mv.visitLabel(nullLabel);
            mv.visitInsn(POP);
            mv.visitLabel(endLabel);
        }
    }

    private void visitForLoopWithArray(ForStatement forLoop, ClassNode componentType) {
        compileStack.pushLoop(forLoop.getVariableScope(), forLoop.getStatementLabel());
        forLoop.getVariable().setType(componentType);

        Label breakLabel = compileStack.getBreakLabel();
        Label continueLabel = compileStack.getContinueLabel();

        int array = compileStack.defineTemporaryVariable("$array$", ClassHelper.OBJECT_TYPE, true);
        mv.visitInsn(ICONST_0);
        int idx = compileStack.defineTemporaryVariable("$idx$", ClassHelper.int_TYPE, true);

        mv.startLoopVisitLabel(continueLabel);
        mv.visitVarInsn(ILOAD, idx);
        mv.visitVarInsn(ALOAD, array);
        mv.visitInsn(ARRAYLENGTH);
        mv.visitJumpInsn(IF_ICMPGE, breakLabel);

        mv.visitVarInsn(ALOAD, array);
        mv.visitVarInsn(ILOAD, idx);
        if (ClassHelper.isPrimitiveType(componentType)) {
                if (componentType == ClassHelper.long_TYPE)
                    mv.visitInsn(LALOAD);
                else
                if (componentType == ClassHelper.float_TYPE)
                    mv.visitInsn(FALOAD);
                else
                if (componentType == ClassHelper.double_TYPE)
                    mv.visitInsn(DALOAD);
                else
                if (componentType == ClassHelper.boolean_TYPE)
                    mv.visitInsn(BALOAD);
                else
                if (componentType == ClassHelper.byte_TYPE)
                    mv.visitInsn(BALOAD);
                else
                if (componentType == ClassHelper.char_TYPE)
                    mv.visitInsn(CALOAD);
                else
                if (componentType == ClassHelper.short_TYPE)
                    mv.visitInsn(SALOAD);
                else
                    mv.visitInsn(IALOAD);
            }
            else
                mv.visitInsn(AALOAD);
        compileStack.defineVariable(forLoop.getVariable(), true);
        forLoop.getLoopBlock().visit(this);

        mv.visitVarInsn(ILOAD, idx);
        mv.visitInsn(ICONST_1);
        mv.visitInsn(IADD);
        mv.visitVarInsn(ISTORE, idx);

        mv.visitJumpInsn(GOTO, continueLabel);

        mv.visitLabel(breakLabel);
        compileStack.pop();
    }

    private void visitForLoopWithIntRange(ForStatement forLoop) {
        compileStack.pushLoop(forLoop.getVariableScope(), forLoop.getStatementLabel());
        forLoop.getVariable().setType(ClassHelper.int_TYPE);

        Label breakLabel = compileStack.getBreakLabel();
        Label continueLabel = compileStack.getContinueLabel();

        mv.visitInsn(DUP);
        int collIdx = compileStack.defineTemporaryVariable("$coll$", ClassHelper.OBJECT_TYPE, true);
        mv.visitTypeInsn(INSTANCEOF, BytecodeHelper.getClassInternalName(EmptyRange.class));
        mv.visitJumpInsn(IFNE, breakLabel);

        mv.visitVarInsn(ALOAD, collIdx);
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "groovy/lang/Range", "getFrom", "()Ljava/lang/Comparable;");
        BytecodeExpr.unbox(ClassHelper.int_TYPE, mv);
        mv.visitVarInsn(ALOAD, collIdx);
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "groovy/lang/Range", "getTo", "()Ljava/lang/Comparable;");
        BytecodeExpr.unbox(ClassHelper.int_TYPE, mv);

        mv.visitVarInsn(ALOAD, collIdx);
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "groovy/lang/Range", "isReverse", "()Z");

        mv.visitInsn(DUP);
        int isReverse = compileStack.defineTemporaryVariable("$isReverse$", ClassHelper.boolean_TYPE, true);
        Label lElse1 = new Label();
        mv.visitJumpInsn(IFEQ, lElse1);
        mv.visitInsn(SWAP);
        mv.visitLabel(lElse1);
        int otherEnd = compileStack.defineTemporaryVariable("$otherEnd$", ClassHelper.int_TYPE, true);
        int thisEnd = compileStack.defineTemporaryVariable("$thisEnd$", ClassHelper.int_TYPE, true);
        Register it = compileStack.defineVariable(forLoop.getVariable(), false);

        mv.startLoopVisitLabel(continueLabel);

        mv.visitVarInsn(ILOAD, otherEnd);
        mv.visitVarInsn(ILOAD, thisEnd);

        Label lElse2 = new Label(), lDone2 = new Label();
        mv.visitVarInsn(ILOAD, isReverse);
        mv.visitJumpInsn(IFNE, lElse2);
        mv.visitJumpInsn(IF_ICMPLT, breakLabel);
        mv.visitJumpInsn(GOTO, lDone2);
        mv.visitLabel(lElse2);
        mv.visitJumpInsn(IF_ICMPGT, breakLabel);
        mv.visitLabel(lDone2);

        mv.visitVarInsn(ILOAD, thisEnd);
        mv.visitInsn(DUP);
        mv.visitVarInsn(ISTORE, it.getIndex());

        mv.visitInsn(ICONST_1);
        mv.visitVarInsn(ILOAD, isReverse);

        Label lElse3 = new Label(), lDone3 = new Label();
        mv.visitJumpInsn(IFNE, lElse3);
        mv.visitInsn(IADD);
        mv.visitJumpInsn(GOTO, lDone3);
        mv.visitLabel(lElse3);
        mv.visitInsn(ISUB);
        mv.visitLabel(lDone3);

        mv.visitVarInsn(ISTORE, thisEnd);

        forLoop.getLoopBlock().visit(this);

        mv.visitJumpInsn(GOTO, continueLabel);

        mv.visitLabel(breakLabel);
        compileStack.pop();
    }

    @Override
    public void visitIfElse(IfStatement ifElse) {
        visitStatement(ifElse);

        final BooleanExpression ifExpr = ifElse.getBooleanExpression();

        Label elseLabel = new Label();

        final BytecodeExpr condition = transformLogical(ifExpr, elseLabel, false);
        condition.visit(mv);

        compileStack.pushBooleanExpression();                                             
        ifElse.getIfBlock().visit(this);
        compileStack.pop();

        Label endLabel = new Label();
        if (ifElse.getElseBlock() != EmptyStatement.INSTANCE) {
            mv.visitJumpInsn(GOTO, endLabel);
        }

        mv.visitLabel(elseLabel);

        if (ifElse.getElseBlock() != EmptyStatement.INSTANCE) {
            compileStack.pushBooleanExpression();
            ifElse.getElseBlock().visit(this);
            compileStack.pop();

            mv.visitLabel(endLabel);
        }
    }

    @Override
    public void visitReturnStatement(ReturnStatement statement) {
        visitStatement(statement);

        Expression returnExpression = statement.getExpression();
        if (!methodNode.getReturnType().equals(ClassHelper.VOID_TYPE) && 
        		!methodNode.getReturnType().equals(ClassHelper.DYNAMIC_TYPE)) {
            if (!shouldImproveReturnType) {
                CastExpression castExpression = new CastExpression(methodNode.getReturnType(), returnExpression);
                castExpression.setSourcePosition(returnExpression);
                returnExpression = castExpression;
            }
            else {
                CastExpression castExpression = new CastExpression(methodNode.getReturnType(), returnExpression);
                castExpression.setSourcePosition(returnExpression);
                returnExpression = castExpression;
            }
        }

        BytecodeExpr bytecodeExpr = (BytecodeExpr) transformToGround(returnExpression);

        if (bytecodeExpr instanceof ResolvedMethodBytecodeExpr) {
            ResolvedMethodBytecodeExpr resolvedMethodBytecodeExpr = (ResolvedMethodBytecodeExpr) bytecodeExpr;
            if (resolvedMethodBytecodeExpr.getMethodNode() == methodNode) {
                if (methodNode.isStatic()
                || resolvedMethodBytecodeExpr.getObject().isThis()
                || methodNode.isPrivate()
                || (methodNode.getModifiers() & ACC_FINAL) != 0) {
                    tailRecursive(resolvedMethodBytecodeExpr);
                    return;
                }
            }
        }

        bytecodeExpr.visit(mv);
        ClassNode exprType = bytecodeExpr.getType();
        ClassNode returnType = methodNode.getReturnType();
        if (returnType.equals(ClassHelper.VOID_TYPE)) {
            compileStack.applyFinallyBlocks();
        } else {
            if (shouldImproveReturnType) {
                if (bytecodeExpr.getType().equals(ClassHelper.VOID_TYPE)) {
                    mv.visitInsn(ACONST_NULL);
                } else {
                    BytecodeExpr.box(exprType, mv);
                    exprType = TypeUtil.wrapSafely(exprType);
                    calculatedReturnType = TypeUtil.commonType(calculatedReturnType, exprType);
                    BytecodeExpr.cast(exprType, calculatedReturnType, mv);
                }
            }
            else {
                if (bytecodeExpr.getType().equals(ClassHelper.VOID_TYPE)) {
                    mv.visitInsn(ACONST_NULL);
                } else {
                    BytecodeExpr.box(exprType, mv);
                    BytecodeExpr.cast(TypeUtil.wrapSafely(exprType), TypeUtil.wrapSafely(returnType), mv);
                }
            }

            if (compileStack.hasFinallyBlocks()) {
                int returnValueIdx = compileStack.defineTemporaryVariable("returnValue", ClassHelper.OBJECT_TYPE, true);
                compileStack.applyFinallyBlocks();
                mv.visitVarInsn(ALOAD, returnValueIdx);
            }
            BytecodeExpr.unbox(returnType, mv);
        }

        bytecodeExpr.doReturn(returnType, mv);
    }

    private void tailRecursive(ResolvedMethodBytecodeExpr resolvedMethodBytecodeExpr) {
        Parameter[] parameters = methodNode.getParameters();

        int varIndex = methodNode.isStatic() ? 0 : 1;
        if (varIndex != 0) {
            resolvedMethodBytecodeExpr.getObject().visit(mv);
        }
        for (int i = 0; i != parameters.length; ++i) {
            BytecodeExpr be = (BytecodeExpr) resolvedMethodBytecodeExpr.getBargs().getExpressions().get(i);
            be.visit(mv);
            final ClassNode paramType = parameters[i].getType();
            final ClassNode type = be.getType();
            BytecodeExpr.box(type, mv);
            BytecodeExpr.cast(TypeUtil.wrapSafely(type), TypeUtil.wrapSafely(paramType), mv);
            BytecodeExpr.unbox(paramType, mv);

            varIndex += (paramType == ClassHelper.long_TYPE || paramType == ClassHelper.double_TYPE) ? 2 : 1;
        }

        for (int i = parameters.length-1; i >= 0; --i) {
            final ClassNode paramType = parameters[i].getType();
            varIndex -= (paramType == ClassHelper.long_TYPE || paramType == ClassHelper.double_TYPE) ? 2 : 1;

            if (paramType == double_TYPE) {
                mv.visitVarInsn(Opcodes.DSTORE, varIndex);
            } else if (paramType == float_TYPE) {
                mv.visitVarInsn(Opcodes.FSTORE, varIndex);
            } else if (paramType == long_TYPE) {
                mv.visitVarInsn(Opcodes.LSTORE, varIndex);
            } else if (
                   paramType == boolean_TYPE
                || paramType == char_TYPE
                || paramType == byte_TYPE
                || paramType == int_TYPE
                || paramType == short_TYPE) {
                mv.visitVarInsn(Opcodes.ISTORE, varIndex);
            } else {
                mv.visitVarInsn(Opcodes.ASTORE, varIndex);
            }
        }

        if (!methodNode.isStatic()) {
            mv.visitVarInsn(ASTORE, 0);
        }
        mv.visitJumpInsn(GOTO, startLabel);
        return;
    }

    @Override
    public void visitWhileLoop(WhileStatement loop) {
        visitStatement(loop);
        compileStack.pushLoop(loop.getStatementLabel());
        Label continueLabel = compileStack.getContinueLabel();
        Label breakLabel = compileStack.getBreakLabel();

        final BytecodeExpr be = transformLogical(loop.getBooleanExpression().getExpression(), breakLabel, false);

        mv.startLoopVisitLabel(continueLabel);
        be.visit(mv);

        loop.getLoopBlock().visit(this);

        mv.visitJumpInsn(GOTO, continueLabel);
        mv.visitLabel(breakLabel);

        compileStack.pop();
    }

    public void visitSwitch(SwitchStatement statement) {
        visitStatement(statement);

        List caseStatements = statement.getCaseStatements();

        BytecodeExpr cond = (BytecodeExpr) transform(statement.getExpression());
        cond.visit(mv);

        if (statement.getExpression() instanceof VariableExpression) {
            final VariableExpression ve = (VariableExpression) statement.getExpression();

            if(!ve.getName().equals("this")) {
                for (Iterator iter = caseStatements.iterator(); iter.hasNext(); ) {
                    CaseStatement caseStatement = (CaseStatement) iter.next();
                    final Expression option = caseStatement.getExpression();
                    if (option instanceof ClassExpression && !ve.getType().equals(ClassHelper.CLASS_Type)) {
                        final BlockStatement newCode = new BlockStatement();
                        final VariableExpression newVar = new VariableExpression(ve.getName(), option.getType());
                        final DeclarationExpression newVarDecl = new DeclarationExpression(
                                newVar,
                                Token.newSymbol(Types.EQUAL, -1, -1),
                                ve
                        );
                        newVarDecl.setSourcePosition(caseStatement);
                        newCode.addStatement(
                                new ExpressionStatement(
                                        newVarDecl
                                )
                        );
                        newCode.addStatement(caseStatement.getCode());
                        caseStatement.setCode(newCode);
                        newCode.visit(new ClassCodeExpressionTransformer() {
                            @Override
                            public Expression transform(Expression exp) {
                                if (exp instanceof VariableExpression) {
                                    VariableExpression vexp = (VariableExpression) exp;
                                    if (vexp.getName().equals(ve.getName())) {
                                        vexp.setAccessedVariable(newVar);
                                    }
                                }
                                return super.transform(exp);
                            }

                            protected SourceUnit getSourceUnit() {
                                return su;
                            }
                        });
                    }
                }
            }
        }

        // switch does not have a continue label. use its parent's for continue
        Label breakLabel = compileStack.pushSwitch();

        int switchVariableIndex = compileStack.defineTemporaryVariable("switch", cond.getType(), true);

        int caseCount = caseStatements.size();
        Label[] codeLabels = new Label[caseCount];
        Label[] condLabels = new Label[caseCount + 1];
        int i;
        for (i = 0; i < caseCount; i++) {
            codeLabels[i] = new Label();
            condLabels[i] = new Label();
        }

        Label defaultLabel = new Label();

        i = 0;
        for (Iterator iter = caseStatements.iterator(); iter.hasNext(); i++) {
            CaseStatement caseStatement = (CaseStatement) iter.next();

            mv.visitLabel(condLabels[i]);

            visitStatement(caseStatement);

            BytecodeExpr.load(cond.getType(), switchVariableIndex, mv);
            BytecodeExpr option = (BytecodeExpr) transformToGround(caseStatement.getExpression());

            if (ClassHelper.isPrimitiveType(option.getType()) && ClassHelper.isPrimitiveType(cond.getType())) {
                option.visit(mv);
                final BytecodeExpr caseValue = new BytecodeExpr(option, option.getType()) {
                    protected void compile(MethodVisitor mv) {
                    }
                };

                final BytecodeExpr switchValue = new BytecodeExpr(cond, cond.getType()) {
                    protected void compile(MethodVisitor mv) {
                    }
                };
                BinaryExpression eq = new BinaryExpression(caseValue, Token.newSymbol(Types.COMPARE_EQUAL, -1, -1), switchValue);
                eq.setSourcePosition(caseValue);
                transformLogical(eq, codeLabels[i], true).visit(mv);
            } else {
                if (ClassHelper.isPrimitiveType(cond.getType())) {
                    if (caseStatement.getExpression() instanceof ClassExpression) {
                        addError("Primitive type " + cond.getType().getName() + " con not be instance of " + ((ClassExpression)caseStatement.getExpression()).getType().getName(), caseStatement.getExpression());
                        continue;
                    }

                    BytecodeExpr.box(cond.getType(), mv);

                    option.visit(mv);
                    BytecodeExpr.box(option.getType(), mv);

                    Label next = i == caseCount - 1 ? defaultLabel : condLabels[i + 1];

                    Label notNull = new Label();
                    mv.visitInsn(DUP);
                    mv.visitJumpInsn(IFNONNULL, notNull);
                    mv.visitJumpInsn(IF_ACMPEQ, codeLabels[i]);
                    mv.visitJumpInsn(GOTO, next);

                    mv.visitLabel(notNull);

                    final BytecodeExpr caseValue = new BytecodeExpr(option, TypeUtil.wrapSafely(option.getType())) {
                        protected void compile(MethodVisitor mv) {
                        }
                    };

                    final BytecodeExpr switchValue = new BytecodeExpr(cond, TypeUtil.wrapSafely(cond.getType())) {
                        protected void compile(MethodVisitor mv) {
                            mv.visitInsn(SWAP);
                        }
                    };
                    MethodCallExpression exp = new MethodCallExpression(caseValue, "isCase", new ArgumentListExpression(switchValue));
                    exp.setSourcePosition(caseValue);
                    transformLogical(exp, codeLabels[i], true).visit(mv);
                } else {
                    if (caseStatement.getExpression() instanceof ClassExpression && 
                    		!cond.getType().equals(ClassHelper.CLASS_Type)) {
                        BytecodeExpr.box(cond.getType(), mv);
                        mv.visitTypeInsn(INSTANCEOF, BytecodeHelper.getClassInternalName(caseStatement.getExpression().getType()));
                        mv.visitJumpInsn(IFNE, codeLabels[i]);
                    }
                    else {
                        option.visit(mv);
                        BytecodeExpr.box(option.getType(), mv);

                        Label next = i == caseCount - 1 ? defaultLabel : condLabels[i + 1];

                        Label notNull = new Label();
                        mv.visitInsn(DUP);
                        mv.visitJumpInsn(IFNONNULL, notNull);
                        mv.visitJumpInsn(IF_ACMPEQ, codeLabels[i]);
                        mv.visitJumpInsn(GOTO, next);

                        mv.visitLabel(notNull);

                        final BytecodeExpr caseValue = new BytecodeExpr(option, TypeUtil.wrapSafely(option.getType())) {
                            protected void compile(MethodVisitor mv) {
                            }
                        };

                        final BytecodeExpr switchValue = new BytecodeExpr(cond, TypeUtil.wrapSafely(cond.getType())) {
                            protected void compile(MethodVisitor mv) {
                                mv.visitInsn(SWAP);
                            }
                        };
                        MethodCallExpression exp = new MethodCallExpression(caseValue, "isCase", new ArgumentListExpression(switchValue));
                        exp.setSourcePosition(caseValue);
                        transformLogical(exp, codeLabels[i], true).visit(mv);
                    }
                }
            }
        }

        mv.visitJumpInsn(GOTO, defaultLabel);

        i = 0;
        for (Iterator iter = caseStatements.iterator(); iter.hasNext(); i++) {
            CaseStatement caseStatement = (CaseStatement) iter.next();
            visitStatement(caseStatement);
            mv.visitLabel(codeLabels[i]);
            caseStatement.getCode().visit(this);
        }

        mv.visitLabel(defaultLabel);
        statement.getDefaultStatement().visit(this);

        mv.visitLineNumber(statement.getLastLineNumber(), new Label());
        mv.visitLabel(breakLabel);

        compileStack.pop();
    }

    @Override
    public void visitCaseStatement(CaseStatement statement) {
    }

    @Override
    public void visitDoWhileLoop(DoWhileStatement loop) {
        visitStatement(loop);

        super.visitDoWhileLoop(loop);
        throw new UnsupportedOperationException();
    }

    @Override
    public void visitSynchronizedStatement(SynchronizedStatement sync) {
        visitStatement(sync);

        sync.setExpression(transform(sync.getExpression()));

        ((BytecodeExpr) sync.getExpression()).visit(mv);
        final int index = compileStack.defineTemporaryVariable("synchronized", ClassHelper.OBJECT_TYPE, true);

        final Label synchronizedStart = new Label();
        final Label synchronizedEnd = new Label();
        final Label catchAll = new Label();

        mv.visitVarInsn(ALOAD, index);
        mv.visitInsn(MONITORENTER);
        mv.visitLabel(synchronizedStart);

        Runnable finallyPart = new Runnable() {
            public void run() {
                mv.visitVarInsn(ALOAD, index);
                mv.visitInsn(MONITOREXIT);
            }
        };
        compileStack.pushFinallyBlock(finallyPart);

        sync.getCode().visit(this);

        finallyPart.run();
        mv.visitJumpInsn(GOTO, synchronizedEnd);
        mv.startExceptionBlock(); // exception variable
        mv.visitLabel(catchAll);
        finallyPart.run();
        mv.visitInsn(ATHROW);
        mv.visitLabel(synchronizedEnd);
        mv.visitInsn(NOP);

        compileStack.popFinallyBlock();
        exceptionBlocks.add(new Runnable() {
            public void run() {
                mv.visitTryCatchBlock(synchronizedStart, catchAll, catchAll, null);
            }
        });
    }

    @Override
    public void visitThrowStatement(ThrowStatement ts) {
        visitStatement(ts);

        super.visitThrowStatement(ts);
        final BytecodeExpr thrown = (BytecodeExpr) ts.getExpression();
        if (!TypeUtil.isDirectlyAssignableFrom(TypeUtil.THROWABLE, thrown.getType())) {
            addError("Only java.lang.Throwable objects may be thrown", thrown);
        }
        thrown.visit(mv);
        mv.visitInsn(ATHROW);
    }

    public void visitContinueStatement(ContinueStatement statement) {
        visitStatement(statement);

        String name = statement.getLabel();
        Label continueLabel = compileStack.getContinueLabel();
        if (name != null) continueLabel = compileStack.getNamedContinueLabel(name);
        if (continueLabel == null) {
            addError("Illegal continue label '" + name + "'", statement);
        }
        compileStack.applyFinallyBlocks(continueLabel, false);
        mv.visitJumpInsn(GOTO, continueLabel);
    }

    public void visitTryCatchFinally(TryCatchStatement statement) {
        visitStatement(statement);

        Statement tryStatement = statement.getTryStatement();
        final Statement finallyStatement = statement.getFinallyStatement();

        int anyExceptionIndex = compileStack.defineTemporaryVariable("exception", DYNAMIC_TYPE, false);
        if (!finallyStatement.isEmpty()) {
            compileStack.pushFinallyBlock(
                    new Runnable() {
                        public void run() {
                            compileStack.pushFinallyBlockVisit(this);
                            finallyStatement.visit(StaticCompiler.this);
                            compileStack.popFinallyBlockVisit(this);
                        }
                    }
            );
        }

        // dummy label to record the local type inference info before try block starts
        final Label dummyLabel = new Label();
        mv.visitLabel(dummyLabel);

        // start try block, label needed for exception table
        final Label tryStart = new Label();
        mv.visitLabel(tryStart);
        tryStatement.visit(this);

        // goto finally part
        final Label finallyStart = new Label();
        mv.visitJumpInsn(GOTO, finallyStart);

        // marker needed for Exception table
        final Label greEnd = new Label();
        mv.visitLabel(greEnd);

        final Label tryEnd = new Label();
        mv.visitLabel(tryEnd);

        for (CatchStatement catchStatement : statement.getCatchStatements()) {
        	mv.comeToLabel(dummyLabel);// restore the type inference info for use in catch blocks
            ClassNode exceptionType = catchStatement.getExceptionType();
            if(exceptionType.getName().equals("java.lang.Exception") && catchStatement.getVariable().isDynamicTyped()) {
                exceptionType = TypeUtil.THROWABLE;
            }
            // start catch block, label needed for exception table
            final Label catchStart = new Label();
            mv.visitLabel(catchStart);

            mv.startExceptionBlock();

            // create exception variable and store the exception
            compileStack.pushState();
            compileStack.defineVariable(catchStatement.getVariable(), true);
            // handle catch body
            catchStatement.visit(this);
            compileStack.pop();
            // goto finally start
            mv.visitJumpInsn(GOTO, finallyStart);
            // add exception to table
            final String exceptionTypeInternalName = BytecodeHelper.getClassInternalName(exceptionType);
            exceptionBlocks.add(new Runnable() {
                public void run() {
                    mv.visitTryCatchBlock(tryStart, tryEnd, catchStart, exceptionTypeInternalName);
                }
            });
        }

        // marker needed for the exception table
        final Label endOfAllCatches = new Label();
        mv.visitLabel(endOfAllCatches);

        // remove the finally, don't let it visit itself
        if (!finallyStatement.isEmpty()) compileStack.popFinallyBlock();

        // start finally
        mv.visitLabel(finallyStart);
        mv.comeToLabel(dummyLabel);// restore the type inference info for use in catch blocks
        finallyStatement.visit(this);
        // goto end of finally
        Label afterFinally = new Label();
        mv.visitJumpInsn(GOTO, afterFinally);

        // start a block catching any Exception
        final Label catchAny = new Label();
        mv.visitLabel(catchAny);
        mv.startExceptionBlock();
        //store exception
        mv.visitVarInsn(ASTORE, anyExceptionIndex);
        finallyStatement.visit(this);
        // load the exception and rethrow it
        mv.visitVarInsn(ALOAD, anyExceptionIndex);
        mv.visitInsn(ATHROW);

        // end of all catches and finally parts
        mv.visitLabel(afterFinally);
        mv.visitInsn(NOP);

        // add catch any block to exception table
        exceptionBlocks.add(new Runnable() {
            public void run() {
                mv.visitTryCatchBlock(tryStart, endOfAllCatches, catchAny, null);
            }
        });
    }

    public void execute() {
        checkWeakerOverriding();
        addReturnIfNeeded();

        setCode(LogicalStatementRewriter.rewrite(getCode(), new VariableScope()));
        LogicalExpressionRewriter.normalize(getCode());

        compileStack.init(methodNode.getVariableScope(), methodNode.getParameters(), mv, methodNode.getDeclaringClass());
        getCode().visit(this);
        compileStack.clear();
        for (Runnable runnable : exceptionBlocks) {
            runnable.run();
        }
    }

    private void checkWeakerOverriding() {
        ClassNode clazz = methodNode.getDeclaringClass();
        if (methodNode.isPublic()) return;
        if (methodNode instanceof ConstructorNode || methodNode.isStaticConstructor()) return;
        ClassNode superClass = clazz.getSuperClass();
        MethodNode superMethod = findSuperMethod(methodNode, clazz, superClass);
        if (superMethod != null && isWeaker(methodNode, superMethod)) {
            addError("Attempting to assign weaker access to " + PresentationUtil.getText(methodNode), methodNode);
        } else {
            for (ClassNode intf : clazz.getInterfaces()) {
                superMethod = findSuperMethod(methodNode, clazz, intf);
                if (superMethod != null && isWeaker(methodNode, superMethod)) {
                    addError("Attempting to assign weaker access to " + PresentationUtil.getText(methodNode), methodNode);
                }
            }
        }    }

    /**
     * precondition: !method.isPublic()
     */
    private boolean isWeaker(MethodNode method, MethodNode superMethod) {
        if (superMethod.isPublic()) return true;
        if (superMethod.isProtected()) return !method.isProtected();
        return false;
    }

    private MethodNode findSuperMethod(MethodNode method, ClassNode clazz, ClassNode superClass) {
        Parameter[] methodParameters = method.getParameters();
        Parameter[] params = new Parameter[methodParameters.length];
        for (int i = 0; i < params.length; i++) {
            ClassNode type = TypeUtil.mapTypeFromSuper(methodParameters[i].getType(), superClass, clazz);
            params[i] = new Parameter(type, methodParameters[i].getName());
        }
        return superClass.getMethod(method.getName(), params);
    }

    public void visitBytecodeSequence(BytecodeSequence sequence) {
        visitStatement(sequence);

        ((BytecodeInstruction) sequence.getInstructions().get(0)).visit(mv);
    }

    public LocalVarInferenceTypes getLocalVarInferenceTypes() {
        return mv.getLocalVarInferenceTypes();
    }

    public void addLocalVarInferenceType(Label label, VariableExpression ve, ClassNode type, int index) {
        mv.addLocalVarInferenceType(label, ve, type, index);
    }
}
