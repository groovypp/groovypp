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
import org.codehaus.groovy.ast.expr.ExpressionTransformer
import org.codehaus.groovy.ast.expr.MapExpression
import org.codehaus.groovy.ast.GroovyCodeVisitor

@Typed class MultiPropertySetExpression extends Expression {
    final Expression object
    final MapExpression properties

    MultiPropertySetExpression (Expression object, MapExpression properties) {
        this.object = object
        this.properties = properties
    }

    Expression transformExpression(ExpressionTransformer transformer) {
        def res = new MultiPropertySetExpression(transformer.transform(object), (MapExpression)transformer.transform(properties))
        res.sourcePosition = this
        return res
    }

    void visit(GroovyCodeVisitor visitor) {
        object.visit(visitor)
        properties.visit(visitor)
    }
}
