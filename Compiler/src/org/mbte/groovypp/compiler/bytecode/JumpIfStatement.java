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
package org.mbte.groovypp.compiler.bytecode;

import org.codehaus.groovy.ast.VariableScope;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.BooleanExpression;
import org.codehaus.groovy.ast.expr.NotExpression;
import org.codehaus.groovy.ast.stmt.*;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;

import java.util.ArrayList;
import java.util.List;

public class JumpIfStatement {
    public final LabelStatement target;
    public final BooleanExpression condition;

    public JumpIfStatement(LabelStatement target, BooleanExpression condition) {
        this.target = target;
        this.condition = condition; // null means goto
    }

    public static Statement rewrite(Statement statement, VariableScope scope) {
        if (statement instanceof IfStatement) {
            final IfStatement ifStatement = (IfStatement) statement;

            List<Statement> list = new ArrayList<Statement>();

            final BooleanExpression condition = ifStatement.getBooleanExpression();
            final BooleanExpression notExpression = condition instanceof NotExpression ? new BooleanExpression(condition.getExpression()) : new NotExpression(condition.getExpression());
            notExpression.setSourcePosition(condition);

            final LabelStatement end = new LabelStatement();

            if (ifStatement.getElseBlock() != EmptyStatement.INSTANCE) {
                // if(C) T else F =>
                //
                // JIF(C) lf
                // T
                // JIF(null) end  - same as GOTO
                // lf:
                // F
                // end:
                final LabelStatement _false = new LabelStatement();
                final ExpressionStatement jis1 = new ExpressionStatement(new JumpIfExpression(condition, _false.labelExpression, notExpression));
                jis1.setSourcePosition(statement);
                list.add(jis1);

                list.add(rewrite(ifStatement.getIfBlock(), scope));
                final ExpressionStatement jis2 = new ExpressionStatement(new JumpIfExpression(ifStatement.getElseBlock(), end.labelExpression, null));
                jis1.setSourcePosition(ifStatement.getElseBlock());
                list.add(jis2);
                list.add(_false);
                list.add(rewrite(ifStatement.getElseBlock(), scope));
            } else {
                // if(C) T =>
                //
                // JIF(!C) end
                // T
                // end:
                // F
                final ExpressionStatement jis = new ExpressionStatement(new JumpIfExpression(condition, end.labelExpression, notExpression));
                jis.setSourcePosition(statement);
                list.add(jis);
                list.add(rewrite(ifStatement.getIfBlock(), scope));
            }
            list.add(end);

            final BlockStatement blockStatement = new BlockStatement(list, scope);
            blockStatement.setSourcePosition(statement);
            return blockStatement;
        }

        if (statement instanceof BlockStatement) {
            final BlockStatement blockStatement = (BlockStatement) statement;
            final List<Statement> statements = blockStatement.getStatements();
            for (int i = 0; i != statements.size(); ++i) {
                statements.set(i, rewrite(statements.get(i), scope));
            }
            return blockStatement;
        }

        if(statement instanceof ForStatement) {
            final ForStatement forStatement = (ForStatement) statement;
            forStatement.setLoopBlock(rewrite(forStatement.getLoopBlock(), forStatement.getVariableScope()));
            return statement;
        }

        if(statement instanceof WhileStatement) {
            final WhileStatement whileStatement = (WhileStatement) statement;
            whileStatement.setLoopBlock(rewrite(whileStatement.getLoopBlock(), scope));
            return statement;
        }

        if(statement instanceof SynchronizedStatement) {
            final SynchronizedStatement synchronizedStatement = (SynchronizedStatement) statement;
            synchronizedStatement.setCode(rewrite(synchronizedStatement.getCode(), scope));
            return statement;
        }

        if(statement instanceof TryCatchStatement) {
            final TryCatchStatement tryCatchStatement = (TryCatchStatement) statement;
            tryCatchStatement.setTryStatement(rewrite(tryCatchStatement.getTryStatement(), scope));
            final Statement finallyStatement = tryCatchStatement.getFinallyStatement();
            if(finallyStatement != null)
                tryCatchStatement.setFinallyStatement(rewrite(finallyStatement, scope));
            final List<CatchStatement> catchStatements = tryCatchStatement.getCatchStatements();
            if(catchStatements != null) {
                for(int i = 0; i != catchStatements.size(); ++i) {
                    final CatchStatement catchStatement = catchStatements.get(i);
                    catchStatement.setCode(rewrite(catchStatement, scope));
                }
            }
            return statement;
        }

        if(statement instanceof SwitchStatement) {
            final SwitchStatement switchStatement = (SwitchStatement) statement;
            switchStatement.setDefaultStatement(rewrite(switchStatement.getDefaultStatement(), scope));

            for(CaseStatement c : switchStatement.getCaseStatements()) {
                c.setCode(rewrite(c.getCode(), scope));
            }
            return statement;
        }

//        if (statement instanceof ExpressionStatement) {
//            final ExpressionStatement expressionStatement = (ExpressionStatement) statement;
//            if (expressionStatement.getExpression() instanceof JumpIfExpression) {
//                final JumpIfExpression expression = (JumpIfExpression) expressionStatement.getExpression();
//                BooleanExpression condition = expression.condition;
//
//                if (condition == null)
//                    return statement;
//
//                if (condition instanceof NotExpression) {
//                    final BooleanExpression newCondition = JumpIfExpression.improveBooleanNot((NotExpression) condition);
//                    if (newCondition != condition) {
//                        final ExpressionStatement res = new ExpressionStatement(new JumpIfExpression(statement, expression.targetExpression, newCondition));
//                        return rewrite(res, scope);
//                    }
//                }
//
//                if (condition.getExpression() instanceof BinaryExpression) {
//                    final BinaryExpression expression = (BinaryExpression) condition.getExpression();
//                    final Token operation = expression.getOperation();
//
//                    if (operation.getType() == Types.LOGICAL_AND) {
//                        // jif a && b, L ==>
//                        //
//                        // jif !a, e
//                        // jif b, L
//                        // e:
//
//                        final LabelStatement end = new LabelStatement();
//                        List<Statement> list = new ArrayList<Statement>();
//
//                        final NotExpression notA = new NotExpression(expression.getLeftExpression());
//                        notA.setSourcePosition(expression.getLeftExpression());
//                        final JumpIfStatement jif1 = new JumpIfStatement(end, JumpIfExpression.improveBooleanNot(notA));
//                        jif1.setSourcePosition(expression.getLeftExpression());
//                        list.add(jif1);
//
//                        final BooleanExpression b = new BooleanExpression(expression.getRightExpression());
//                        b.setSourcePosition(expression.getRightExpression());
//                        final JumpIfStatement jif2 = new JumpIfStatement(jumpIfStatement.target, b);
//                        jif2.setSourcePosition(expression.getRightExpression());
//                        list.add(jif2);
//
//                        list.add(end);
//
//                        final BlockStatement blockStatement = new BlockStatement(list, scope);
//                        return rewrite(blockStatement, scope);
//                    }
//
//                    if (operation.getType() == Types.LOGICAL_OR) {
//                        // jif a || b, L ==>
//                        //
//                        // jif a, L
//                        // jif b, L
//
//                        List<Statement> list = new ArrayList<Statement>();
//
//                        final BooleanExpression a = new BooleanExpression(expression.getLeftExpression());
//                        a.setSourcePosition(expression.getLeftExpression());
//                        final JumpIfStatement jif1 = new JumpIfStatement(jumpIfStatement.target, a);
//                        jif1.setSourcePosition(expression.getLeftExpression());
//                        list.add(jif1);
//
//                        final BooleanExpression b = new BooleanExpression(expression.getRightExpression());
//                        b.setSourcePosition(expression.getRightExpression());
//                        final JumpIfStatement jif2 = new JumpIfStatement(jumpIfStatement.target, b);
//                        jif2.setSourcePosition(expression.getRightExpression());
//                        list.add(jif2);
//
//                        final BlockStatement blockStatement = new BlockStatement(list, scope);
//                        return rewrite(blockStatement, scope);
//                    }
//                }
//            }
//        }

        return statement;
    }

}
