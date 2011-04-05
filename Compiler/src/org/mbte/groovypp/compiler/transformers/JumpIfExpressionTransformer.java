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
package org.mbte.groovypp.compiler.transformers;

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.mbte.groovypp.compiler.bytecode.ExpressionList;
import org.mbte.groovypp.compiler.bytecode.JumpIfExpression;
import org.mbte.groovypp.compiler.bytecode.LabelExpression;
import org.objectweb.asm.MethodVisitor;

public class JumpIfExpressionTransformer extends ExprTransformer<JumpIfExpression> {

    public Expression transform(final JumpIfExpression exp, CompilerTransformer compiler) {
        if(exp.condition == null) {
            return new BytecodeExpr(exp, ClassHelper.VOID_TYPE) {
                protected void compile(MethodVisitor mv) {
                    mv.visitJumpInsn(GOTO, exp.targetExpression.label);
                }
            };
        }

        if(exp.condition instanceof NotExpression) {
            final BooleanExpression newCondition = improveBooleanNot((NotExpression) exp.condition);
            if(newCondition != exp.condition) {
                return compiler.transform(new JumpIfExpression(exp.condition, exp.targetExpression, newCondition));
            }
        }

        if(exp.condition.getExpression() instanceof BinaryExpression) {
            final BinaryExpression expression = (BinaryExpression) exp.condition.getExpression();
            final Token operation = expression.getOperation();

            // jif(a && b) L =>
            //
            //      jif !a, end
            //      jif b, L
            // end:
            //
            if(operation.getType() == Types.LOGICAL_AND) {
                final ExpressionList expressionList = new ExpressionList(expression, ClassHelper.VOID_TYPE);

                final NotExpression nl = new NotExpression(expression.getLeftExpression());
                nl.setSourcePosition(expression.getLeftExpression());
                LabelExpression end = new LabelExpression(expression.getRightExpression());
                expressionList.expressions.add(new JumpIfExpression(expression.getLeftExpression(), end, nl));

                expressionList.expressions.add(new JumpIfExpression(expression.getRightExpression(), exp.targetExpression, new BooleanExpression(expression.getRightExpression())));

                expressionList.expressions.add(end);

                return compiler.transform(expressionList);
            }

            // jif(a || b) L =>
            //
            //      jif a, L
            //      jif b, L
            //
            if(operation.getType() == Types.LOGICAL_OR) {
                final ExpressionList expressionList = new ExpressionList(expression, ClassHelper.VOID_TYPE);

                expressionList.expressions.add(new JumpIfExpression(expression.getLeftExpression(), exp.targetExpression, new BooleanExpression(expression.getLeftExpression())));
                expressionList.expressions.add(new JumpIfExpression(expression.getRightExpression(), exp.targetExpression, new BooleanExpression(expression.getRightExpression())));

                return compiler.transform(expressionList);
            }
        }

        return compiler.transformLogical(exp.condition, exp.targetExpression.label, true);
    }

    public static BooleanExpression improveBooleanNot(NotExpression condition) {
        if(condition.getExpression() instanceof NotExpression) {
            // !!a => a
            final BooleanExpression res = new BooleanExpression(((NotExpression) condition.getExpression()).getExpression());
            res.setSourcePosition(res);
            return res;
        }

        if(condition.getExpression() instanceof BinaryExpression) {
            // !(a && b) => !a || !b
            // !(a || b) => !a && !b
            final BinaryExpression expression = (BinaryExpression) condition.getExpression();
            final Token operation = expression.getOperation();

            if(operation.getType() == Types.LOGICAL_AND || operation.getType() == Types.LOGICAL_OR) {
                final NotExpression nl = new NotExpression(expression.getLeftExpression());
                nl.setSourcePosition(expression.getLeftExpression());

                final NotExpression nr = new NotExpression(expression.getRightExpression());
                nl.setSourcePosition(expression.getLeftExpression());

                final int newType = operation.getType() == Types.LOGICAL_AND ? Types.LOGICAL_OR : Types.LOGICAL_AND;

                final BinaryExpression bin = new BinaryExpression(nl, Token.newSymbol(newType, operation.getStartLine(), operation.getStartColumn()), nr);
                bin.setSourcePosition(condition);

                final BooleanExpression res = new BooleanExpression(bin);
                res.setSourcePosition(condition);

                return res;
            }
        }

        if(condition.getExpression() instanceof TernaryExpression) {
            final TernaryExpression ternaryExpression = (TernaryExpression) condition.getExpression();
            if(ternaryExpression instanceof ElvisOperatorExpression) {
                throw new RuntimeException("Negation of Elvis operator is not yet supported");
            }

            final NotExpression nt = new NotExpression(ternaryExpression.getTrueExpression());
            nt.setSourcePosition(ternaryExpression.getTrueExpression());

            final NotExpression nf = new NotExpression(ternaryExpression.getFalseExpression());
            nf.setSourcePosition(ternaryExpression.getFalseExpression());

            final TernaryExpression resExpression = new TernaryExpression(ternaryExpression.getBooleanExpression(), nt, nf);
            resExpression.setSourcePosition(ternaryExpression);

            final BooleanExpression res = new BooleanExpression(resExpression);
            res.setSourcePosition(condition);

            return res;
        }

        return condition;
    }
}
