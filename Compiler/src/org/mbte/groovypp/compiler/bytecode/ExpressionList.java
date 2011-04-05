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

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.ExpressionTransformer;
import org.objectweb.asm.MethodVisitor;

import java.util.ArrayList;
import java.util.List;

public class ExpressionList extends Expression {
    public final List<Expression> expressions = new ArrayList<Expression>();

    public ExpressionList(ASTNode parent, ClassNode type) {
        setSourcePosition(parent);
        setType(type);
    }

    @Override
    public Expression transformExpression(ExpressionTransformer transformer) {
        final ExpressionList expressionList = new ExpressionList(this, getType());
        for(Expression e : expressions) {
            expressionList.expressions.add(transformer.transform(e));
        }
        return expressionList;
    }
}
