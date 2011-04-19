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
package org.mbte.groovypp.compiler.transformers

import org.mbte.groovypp.compiler.flow.MultiPropertySetExpression
import org.codehaus.groovy.ast.expr.Expression
import org.mbte.groovypp.compiler.CompilerTransformer
import org.mbte.groovypp.compiler.flow.ExpressionList
import org.codehaus.groovy.ast.expr.PropertyExpression
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr
import org.codehaus.groovy.ast.expr.BinaryExpression
import org.codehaus.groovy.syntax.Token
import org.codehaus.groovy.syntax.Types
import org.codehaus.groovy.ast.ClassHelper
import org.mbte.groovypp.compiler.TypeUtil

@Typed class MultiPropertySetExpressionTransformer extends ExprTransformer<MultiPropertySetExpression>{

    Expression transform(MultiPropertySetExpression exp, CompilerTransformer compiler) {
        def bobj = compiler.transform(exp.object)
        ExpressionList result = [exp, bobj.type]

//        if(bobj.type.isDerivedFrom(TypeUtil.FOBJECT))

        result << bobj
        for(e in exp.properties.mapEntryExpressions) {
            BytecodeExpr obj = [
                'super' : [e.keyExpression, bobj.type],
                 compile: { mv ->
                     mv.visitInsn DUP
                 }
            ]
            PropertyExpression p = [obj, e.keyExpression]
            p.sourcePosition = e.keyExpression

            BinaryExpression assign = [p, Token.newSymbol(Types.ASSIGN, e.valueExpression.lineNumber, e.valueExpression.columnNumber), e.valueExpression]
            def bassign = compiler.transform(assign)

            result << bassign

            BytecodeExpr pop = [
                'super' : [e.valueExpression, ClassHelper.VOID_TYPE],
                 compile: { mv ->
                     pop(bassign.type, mv)
                 }
            ]

            result << pop
        }

        compiler.transform(result)
    }
}
