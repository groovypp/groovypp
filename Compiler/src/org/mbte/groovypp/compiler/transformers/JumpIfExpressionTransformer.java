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
import org.mbte.groovypp.compiler.flow.ExpressionList;
import org.mbte.groovypp.compiler.flow.JumpIfExpression;
import org.mbte.groovypp.compiler.flow.LabelExpression;
import org.objectweb.asm.MethodVisitor;

public class JumpIfExpressionTransformer extends ExprTransformer<JumpIfExpression> {

    public Expression transform(final JumpIfExpression exp, CompilerTransformer compiler) {
        if (exp.condition == null) {
            return new BytecodeExpr(exp, ClassHelper.VOID_TYPE) {
                protected void compile(MethodVisitor mv) {
                    mv.visitJumpInsn(GOTO, exp.targetExpression.label);
                }
            };
        }

        return compiler.transformLogical(exp.condition, exp.targetExpression.label, true);
    }
}
