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

import org.codehaus.groovy.ast.expr.MapExpression
import org.codehaus.groovy.ast.expr.MapEntryExpression
import org.codehaus.groovy.ast.expr.ListExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.ExpressionTransformer

@Typed class MapWithListExpression extends MapExpression {
    public final ListExpression listExpression

    public MapWithListExpression(List<MapEntryExpression> mapEntryExpressions, ListExpression listExpression) {
        super(mapEntryExpressions)
        this.listExpression = listExpression
    }

    Expression transformExpression(ExpressionTransformer transformer) {
        Expression ret = new MapWithListExpression(transformExpressions(getMapEntryExpressions(), transformer, MapEntryExpression.class), (ListExpression)transformer.transform(listExpression));
        ret.setSourcePosition(this);
        return ret;
    }
}
