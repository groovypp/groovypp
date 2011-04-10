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
package org.mbte.groovypp.compiler.flow

import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.NotExpression
import org.codehaus.groovy.ast.expr.BinaryExpression
import org.codehaus.groovy.ast.expr.BooleanExpression
import org.codehaus.groovy.syntax.Types

import org.codehaus.groovy.ast.ClassCodeExpressionTransformer
import org.codehaus.groovy.ast.expr.TernaryExpression
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.ast.ClassHelper

@Typed abstract class LogicalExpressionRewriter {
    private static final ClassCodeExpressionTransformer normalizer = [
        getSourceUnit: { null },

        transform: { src ->
            src = src?.transformExpression(this)

            switch(src) {
                case NotExpression:
                    def res = negate(src.expression)
                    res.sourcePosition = src
                    return res

                case BinaryExpression:
                    switch (src.operation.type) {
                        case Types.LOGICAL_AND:
                            def res = new AndExpression(src.leftExpression, src.rightExpression, src.operation)
                            res.sourcePosition = src
                            return res

                        case Types.LOGICAL_OR:
                            def res = new OrExpression(src.leftExpression, src.rightExpression, src.operation)
                            res.sourcePosition = src
                            return res

                        default:
                            return src
                    }
                break

                case JumpIfExpression:
                    def condition = src.condition
                    switch(condition) {
                        case AndExpression:
                            // jif a && b, L =>
                            //
                            // jif !a, end
                            // jif b, L
                            // end:
                            LabelExpression end = [src]
                            ExpressionList res = [src, ClassHelper.VOID_TYPE]

                            JumpIfExpression na = [end, negate(condition.left)]
                            na.sourcePosition = condition.left

                            JumpIfExpression  b = [src.targetExpression, condition.right]
                            b.sourcePosition = condition.right

                            // @todo probably unneded recursion here
                            return res << normalize(na) << b << end

                        case OrExpression:
                            // jif a || b, L =>
                            //
                            // jif a, L
                            // jif b, L
                            ExpressionList res = [src, ClassHelper.VOID_TYPE]

                            JumpIfExpression a = [src.targetExpression, condition.left]
                            a.sourcePosition = condition.left

                            JumpIfExpression  b = [src.targetExpression, condition.right]
                            b.sourcePosition = condition.right

                            return res << a << b

                        default:
                            return src
                    }
                break

                default:
                    return src
            }
        }
    ]

    static void normalize(Statement src) {
        src.visit normalizer
    }

    static Expression normalize(Expression src) {
        normalizer.transform src
    }

    static Expression negate(Expression src)  {
        switch(src) {
            case TernaryExpression:
                TernaryExpression res = [src.booleanExpression, negate(src.trueExpression), src.falseExpression]
                res.sourcePosition = src
                return res

            case NotExpression:
                BooleanExpression res = [src.expression]
                res.sourcePosition = src.expression
                return res

            case BooleanExpression:
                BooleanExpression res = [negate(src.expression)]
                res.sourcePosition = src
                return res

            case AndExpression:
                OrExpression res = [negate(src.left), negate(src.right), src.operation]
                res.sourcePosition = src
                return res

            case OrExpression:
                AndExpression res = [negate(src.left), negate(src.right), src.operation]
                res.sourcePosition = src
                return res

            default:
                NotExpression res = [src]
                res.sourcePosition = src
                return res
        }
    }
}
