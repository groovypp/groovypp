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

import org.codehaus.groovy.ast.expr.Expression;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.mbte.groovypp.compiler.bytecode.ExpressionList;
import org.objectweb.asm.MethodVisitor;

import java.util.ArrayList;

public class ExpressionListTransformer extends ExprTransformer<ExpressionList> {

    public Expression transform(ExpressionList exp, CompilerTransformer compiler) {
        final ArrayList<BytecodeExpr> list = new ArrayList<BytecodeExpr>(exp.expressions.size());
        for(Expression e : exp.expressions) {
            list.add((BytecodeExpr) compiler.transform(e));
        }

        return new BytecodeExpr(exp, exp.getType()) {
            protected void compile(MethodVisitor mv) {
                for(BytecodeExpr b : list) {
                    b.visit(mv);
                }
            }
        };
    }
}
