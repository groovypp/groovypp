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
package org.mbte.groovypp.compiler.flow

import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.GroovyCodeVisitor
import org.codehaus.groovy.ast.expr.ExpressionTransformer
import org.codehaus.groovy.ast.expr.Expression

@Typed class ExpressionList extends Expression {
    public final List<Expression> expressions = []

    ExpressionList(ASTNode parent, ClassNode type) {
        sourcePosition = parent
        setType(type)
    }

    Expression transformExpression(ExpressionTransformer transformer) {
        def expressionList = new ExpressionList(this, type)
        for(e in expressions) {
            expressionList << transformer.transform(e)
        }
        expressionList
    }

    void visit(GroovyCodeVisitor visitor) {
        for(Expression e : expressions) {
            e.visit(visitor);
        }
    }

    ExpressionList leftShift(Expression exp) {
        if(exp instanceof ExpressionList) {
            expressions.addAll(((ExpressionList)exp).expressions)
        }
        else {
            expressions.add(exp)
        }
        this
    }
}
