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
import org.mbte.groovypp.compiler.bytecode.PropertyUtil
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.PropertyNode
import org.codehaus.groovy.ast.expr.CastExpression
import org.objectweb.asm.MethodVisitor
import org.codehaus.groovy.ast.expr.MapEntryExpression
import org.codehaus.groovy.ast.expr.ConstantExpression

@Typed class MultiPropertySetExpressionTransformer extends ExprTransformer<MultiPropertySetExpression>{

     static class Untransformed extends BytecodeExpr {
        final MultiPropertySetExpression exp;
        final Expression object

        Untransformed(MultiPropertySetExpression exp, CompilerTransformer compiler, Expression object) {
            super(exp, object.type)
            this.exp = exp
            this.object = object
        }

        protected void compile(MethodVisitor mv) {
            throw new UnsupportedOperationException()
        }

        public BytecodeExpr transform (CompilerTransformer compiler) {
            compiler.transform(new MultiPropertySetExpression(compiler.transformToGround(object), exp.properties) [sourcePosition: exp])
        }
    }

    Expression transform(MultiPropertySetExpression exp, CompilerTransformer compiler) {
        Expression bobj = compiler.transform(exp.object)

        if (bobj instanceof ListExpressionTransformer.Untransformed ||
            bobj instanceof MultiPropertySetExpressionTransformer.Untransformed ||
            bobj instanceof MapExpressionTransformer.Untransformed ||
            bobj instanceof MapWithListExpressionTransformer.Untransformed ||
            bobj instanceof TernaryExpressionTransformer.Untransformed) {
            return new Untransformed(exp, compiler, bobj)
        }

        ExpressionList result = [exp, bobj.type]

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

            BytecodeExpr bassign

            def keyName = ((ConstantExpression)e.keyExpression).value.toString()
            def prop = PropertyUtil.resolveSetProperty(bobj.type, keyName, TypeUtil.NULL_TYPE, compiler, true);
            if (prop != null) {
                ClassNode propType;
                ClassNode propDeclClass;
                if (prop instanceof MethodNode) {
                    propType = prop.parameters[0].type
                    propDeclClass = prop.declaringClass
                }
                else
                    if (prop instanceof FieldNode) {
                        propType = prop.type
                        propDeclClass = prop.declaringClass
                    }
                    else {
                        propType = ((PropertyNode)prop).type
                        propDeclClass = ((PropertyNode)prop).declaringClass
                    }

                propType = TypeUtil.getSubstitutedType(propType, propDeclClass, obj.type)

                final CastExpression cast = [propType, e.valueExpression]
                cast.sourcePosition = e.valueExpression

                bassign = PropertyUtil.createSetProperty(e, compiler, keyName, obj, (BytecodeExpr) compiler.transform(cast), prop)
            }
            else {
                // at least we can try
                BinaryExpression assign = [p, Token.newSymbol(Types.ASSIGN, e.valueExpression.lineNumber, e.valueExpression.columnNumber), e.valueExpression]
                bassign = compiler.transform(assign)
            }

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
