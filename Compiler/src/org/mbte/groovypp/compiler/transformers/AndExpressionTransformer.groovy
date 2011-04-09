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

import org.mbte.groovypp.compiler.bytecode.AndExpression
import org.codehaus.groovy.ast.expr.Expression
import org.mbte.groovypp.compiler.CompilerTransformer
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr
import org.objectweb.asm.Label
import org.codehaus.groovy.ast.ClassHelper
import org.objectweb.asm.MethodVisitor

@Typed class AndExpressionTransformer extends ExprTransformer<AndExpression> {
    Expression transform(AndExpression exp, CompilerTransformer compiler) {
        new BinaryExpressionTransformer.Logical(exp, compiler)
    }

    BytecodeExpr transformLogical(AndExpression exp, CompilerTransformer compiler, Label label, boolean onTrue) {
        if (onTrue) {
            final Label _false = new Label();
            final BytecodeExpr l = unboxReference(exp, compiler.transformLogical(exp.left, _false, false), compiler);
            final BytecodeExpr r = unboxReference(exp, compiler.transformLogical(exp.right, label, true), compiler);
            return new BytecodeExpr(exp, ClassHelper.VOID_TYPE) {
                protected void compile(MethodVisitor mv) {
                    l.visit(mv);
                    r.visit(mv);
                    mv.visitLabel(_false);
                }
            };
        } else {
            final BytecodeExpr l = unboxReference(exp, compiler.transformLogical(exp.left, label, false), compiler);
            final BytecodeExpr r = unboxReference(exp, compiler.transformLogical(exp.right, label, false), compiler);
            return new BytecodeExpr(exp, ClassHelper.VOID_TYPE) {
                protected void compile(MethodVisitor mv) {
                    l.visit(mv);
                    r.visit(mv);
                }
            };
        }
    }
}
