/*
 * Copyright 2009-2011 MBTE Sweden AB.
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
package org.mbte.groovypp.compiler;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.DeclarationExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.*;
import org.codehaus.groovy.control.SourceUnit;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.mbte.groovypp.compiler.transformers.DeclarationExpressionTransformer;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.List;

public abstract class LabeledClosureExtractor extends ClassCodeVisitorSupport implements Opcodes{
    private final SourceUnit source;
    private ClassNode classNode;

    public LabeledClosureExtractor(SourceUnit source, ClassNode classNode) {
        this.source = source;
        this.classNode = classNode;
    }

    protected SourceUnit getSourceUnit() {
        return source;
    }

    public void visitClosureExpression(ClosureExpression expression) {
    }

    public void visitBlockStatement(BlockStatement block) {
        List<Statement> statements = block.getStatements();
        for(int i = 0; i != statements.size(); ++i) {
            Statement statement = statements.get(i);

            // @Field?
//            if(statement instanceof ExpressionStatement) {
//                final ExpressionStatement expressionStatement = (ExpressionStatement) statement;
//                final Expression expression = expressionStatement.getExpression();
//                if (processDeclarationExpression(statements, i, expression))
//                    continue;
//            }
//
//            if(statement instanceof ReturnStatement) {
//                final ReturnStatement returnStatement = (ReturnStatement) statement;
//                final Expression expression = returnStatement.getExpression();
//                if (processDeclarationExpression(statements, i, expression))
//                    continue;
//            }

            // labeled closure?
//            if(statement.getStatementLabel() != null) {
//                if(statement instanceof ExpressionStatement) {
//                    ExpressionStatement expressionStatement = (ExpressionStatement) statement;
//                    if(expressionStatement.getExpression() instanceof ClosureExpression) {
//                        ClosureExpression closureExpression = (ClosureExpression) expressionStatement.getExpression();
//                        Parameter[] parameters = closureExpression.getParameters();
//                        if(parameters == null)
//                            parameters = Parameter.EMPTY_ARRAY;
//                        MethodNode methodNode = classNode.addMethod(statement.getStatementLabel(), ACC_PUBLIC | ACC_FINAL, TypeUtil.IMPROVE_TYPE, parameters, null, null);
//                        methodNode.setCode(closureExpression.getCode());
//                        methodNode.setSourcePosition(statement);
//                        onExtractedMethod(methodNode);
//                        statements.set(i, EmptyStatement.INSTANCE);
//                        continue;
//                    }
//                }
//
//                if(statement instanceof BlockStatement) {
//                    BlockStatement blockStatement = (BlockStatement) statement;
//                    MethodNode methodNode = classNode.addMethod(blockStatement.getStatementLabel(), ACC_PUBLIC | ACC_FINAL, TypeUtil.IMPROVE_TYPE, Parameter.EMPTY_ARRAY, null, null);
//                    methodNode.setCode(blockStatement);
//                    methodNode.setSourcePosition(statement);
//                    blockStatement.setStatementLabel(null);
//                    onExtractedMethod(methodNode);
//                    statements.set(i, EmptyStatement.INSTANCE);
//                }
//            }
        }

        // we don't go inside because labeled methods and @Fields allowed only on top level
        // super.visitBlockStatement(block);
    }

    private boolean processDeclarationExpression(List<Statement> statements, int i, Expression expression) {
        if(expression instanceof DeclarationExpression) {
            final DeclarationExpression exp = (DeclarationExpression) expression;
            final VariableExpression ve = exp.getVariableExpression();
            if(DeclarationExpressionTransformer.hasFieldAnnotation(ve)) {
                ClassNode type = ve.getOriginType().equals(ClassHelper.DYNAMIC_TYPE) ? TypeUtil.IMPROVE_TYPE : ve.getOriginType();
                FieldNode fieldNode = classNode.addField(ve.getName(), ACC_PRIVATE, type, exp.getRightExpression());
                ClassNodeCache.clearCache(classNode);
                if(classNode instanceof ClosureClassNode)
                    fieldNode.addAnnotation(new AnnotationNode(TypeUtil.NO_EXTERNAL_INITIALIZATION));
                else {
                    CleaningVerifier.getCleaningVerifier().addInitialization(classNode);
                }
                ve.setAccessedVariable(fieldNode);
                return true;
            }
        }
        return false;
    }

    protected abstract void onExtractedMethod(MethodNode methodNode);
}
