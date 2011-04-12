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


import org.codehaus.groovy.ast.expr.*
import org.codehaus.groovy.ast.GroovyCodeVisitor

@Typed class JumpIfExpression extends Expression {
    public final LabelExpression targetExpression
    public final BooleanExpression condition // null means GOTO

    public JumpIfExpression(LabelExpression labelExpression, Expression condition) {
        this.targetExpression = labelExpression;
        this.condition = condition instanceof BooleanExpression ? condition : ( condition ? [condition] : null)
    }

    public Expression transformExpression(ExpressionTransformer transformer) {
        JumpIfExpression res = [targetExpression, condition != null ? (BooleanExpression)transformer.transform(condition) : null]
        res.sourcePosition = this
        res
    }

    void visit(GroovyCodeVisitor visitor) {
        condition.visit(visitor)
    }
}
