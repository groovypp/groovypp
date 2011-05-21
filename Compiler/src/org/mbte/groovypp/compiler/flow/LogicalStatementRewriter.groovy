/*
 * Copyright 2009-2010 MBTE Sweden AB.
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
package org.mbte.groovypp.compiler.flow;

import org.codehaus.groovy.ast.VariableScope;


import org.codehaus.groovy.ast.expr.BooleanExpression;
import org.codehaus.groovy.ast.stmt.*

@Typed abstract class LogicalStatementRewriter {
    public static Statement rewrite(Statement statement, VariableScope scope) {
        switch(statement) {
            case IfStatement:
                ArrayList<Statement> list = []

                def condition = statement.booleanExpression
                def notExpression = LogicalExpressionRewriter.negate(condition)
                notExpression.sourcePosition = condition

                LabelStatement end = []

                if (statement.elseBlock != EmptyStatement.INSTANCE) {
                    if(statement.ifBlock != EmptyStatement.INSTANCE) {
                        // if(C) T else F =>
                        //
                        // JIF(C) lf
                        // T
                        // JIF(null) end  - same as GOTO
                        // lf:
                        // F
                        // end:
                        LabelStatement _false = []
                        ExpressionStatement jis1 = [new JumpIfExpression(_false.labelExpression, (BooleanExpression)notExpression)]
                        jis1.expression.sourcePosition = condition
                        jis1.sourcePosition = statement
                        list << jis1

                        list.add(rewrite(statement.ifBlock, scope));
                        ExpressionStatement jis2 = [new JumpIfExpression(end.labelExpression, null)];
                        jis1.expression.sourcePosition = statement.elseBlock
                        jis1.sourcePosition = statement.elseBlock
                        list << jis2 << _false << rewrite(statement.elseBlock, scope)
                    }
                    else {
                        // if(C) {} else F =>
                        //
                        // JIF(C) end
                        // F
                        // end:
                        final ExpressionStatement jis = [new JumpIfExpression(end.labelExpression, condition)]
                        jis.expression.sourcePosition = condition
                        jis.sourcePosition = statement
                        list << jis << rewrite(statement.elseBlock, scope)
                    }
                } else {
                    // if(C) T =>
                    //
                    // JIF(!C) end
                    // T
                    // end:
                    final ExpressionStatement jis = [new JumpIfExpression(end.labelExpression, (BooleanExpression)notExpression)]
                    jis.expression.sourcePosition = condition
                    jis.sourcePosition = statement
                    list << jis << rewrite(statement.ifBlock, scope)
                }
                list.add(end);

                final BlockStatement blockStatement = [list, scope]
                blockStatement.sourcePosition = statement
                return blockStatement

            case BlockStatement:
                final List<Statement> statements = statement.getStatements();
                for (int i = 0; i != statements.size(); ++i) {
                    statements.set(i, rewrite(statements.get(i), scope));
                }
            break

            case ForStatement:
                statement.loopBlock = rewrite(statement.loopBlock, statement.getVariableScope())
            break

            case WhileStatement:
                statement.loopBlock = rewrite(statement.loopBlock, scope);
            break

            case SynchronizedStatement:
                statement.code = rewrite(statement.code, scope)
            break

            case TryCatchStatement:
                statement.tryStatement = rewrite(statement.tryStatement, scope)
                def finallyStatement = statement.finallyStatement
                if(finallyStatement != null)
                    statement.finallyStatement = rewrite(finallyStatement, scope)
                final List<CatchStatement> catchStatements = statement.catchStatements
                if(catchStatements != null) {
                    for(int i = 0; i != catchStatements.size(); ++i) {
                        def catchStatement = catchStatements.get(i);
                        catchStatement.code = rewrite(catchStatement.code, scope)
                    }
                }
            break

            case SwitchStatement:
                statement.defaultStatement = rewrite(statement.defaultStatement, scope)

                for(c in statement.caseStatements) {
                    c.code = rewrite(c.getCode(), scope)
                }
            break
        }

        return statement
    }
}
