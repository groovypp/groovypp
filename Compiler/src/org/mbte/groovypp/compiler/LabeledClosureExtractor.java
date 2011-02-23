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

import org.codehaus.groovy.ast.ClassCodeVisitorSupport;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.EmptyStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.control.SourceUnit;
import org.objectweb.asm.Opcodes;

import java.util.List;

public abstract class LabeledClosureExtractor extends ClassCodeVisitorSupport implements Opcodes{
    private final SourceUnit source;
    private ClassNode declaringClass;

    public LabeledClosureExtractor(SourceUnit source, ClassNode declaringClass) {
        this.source = source;
        this.declaringClass = declaringClass;
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
            if(statement.getStatementLabel() != null) {
                if(statement instanceof ExpressionStatement) {
                    ExpressionStatement expressionStatement = (ExpressionStatement) statement;
                    if(expressionStatement.getExpression() instanceof ClosureExpression) {
                        ClosureExpression closureExpression = (ClosureExpression) expressionStatement.getExpression();
                        Parameter[] parameters = closureExpression.getParameters();
                        if(parameters == null)
                            parameters = Parameter.EMPTY_ARRAY;
                        MethodNode methodNode = declaringClass.addMethod(statement.getStatementLabel(), ACC_PUBLIC | ACC_FINAL, TypeUtil.IMPROVE_TYPE, parameters, null, null);
                        methodNode.setCode(closureExpression.getCode());
                        methodNode.setSourcePosition(statement);
                        onExtractedMethod(methodNode);
                        statements.set(i, EmptyStatement.INSTANCE);
                    }
                }

                if(statement instanceof BlockStatement) {
                    BlockStatement blockStatement = (BlockStatement) statement;
                    MethodNode methodNode = declaringClass.addMethod(blockStatement.getStatementLabel(), ACC_PUBLIC | ACC_FINAL, TypeUtil.IMPROVE_TYPE, Parameter.EMPTY_ARRAY, null, null);
                    methodNode.setCode(blockStatement);
                    methodNode.setSourcePosition(statement);
                    blockStatement.setStatementLabel(null);
                    onExtractedMethod(methodNode);
                    statements.set(i, EmptyStatement.INSTANCE);
                }
            }
        }
        super.visitBlockStatement(block);
    }

    protected abstract void onExtractedMethod(MethodNode methodNode);
}
