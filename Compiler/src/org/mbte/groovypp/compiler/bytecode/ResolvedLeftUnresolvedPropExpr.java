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
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.syntax.Token;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.TypeUtil;
import org.objectweb.asm.MethodVisitor;

public class ResolvedLeftUnresolvedPropExpr extends ResolvedLeftExpr {
    private final BytecodeExpr object;
    private final String propName;
    private final BytecodeExpr delegate;

    public ResolvedLeftUnresolvedPropExpr(ASTNode parent, BytecodeExpr object, String propName, CompilerTransformer compiler) {
        super(parent, ClassHelper.OBJECT_TYPE);
        MethodCallExpression call = new MethodCallExpression(object, "getUnresolvedProperty", new ArgumentListExpression(new ConstantExpression(propName)));
        call.setSourcePosition(parent);
        delegate = (BytecodeExpr) compiler.transform(call);
        setType(delegate.getType());
        this.object = object;
        this.propName = propName;
    }

    public BytecodeExpr createAssign(ASTNode parent, BytecodeExpr right, CompilerTransformer compiler) {
        Object prop = PropertyUtil.resolveSetProperty(object.getType(), propName, getType(), compiler, isThis());
        if(prop != null) {
            final CastExpression cast = new CastExpression(getType(), right);
            cast.setSourcePosition(right);
            right = (BytecodeExpr) compiler.transform(cast);
            return PropertyUtil.createSetProperty(parent, compiler, propName, object, right, prop);
        }
        else {
            MethodNode method = compiler.findMethod(object.getType(), "setUnresolvedProperty", new ClassNode[]{ClassHelper.STRING_TYPE, getType()}, false);
            if(method != null && method.getReturnType().equals(ClassHelper.VOID_TYPE)) {
                MethodCallExpression exp = new MethodCallExpression(new BytecodeExpr(object, object.getType()){
                    protected void compile(MethodVisitor mv) {
                    }
                }, "setUnresolvedProperty", new ArgumentListExpression(new BytecodeExpr(parent, ClassHelper.STRING_TYPE){
                    protected void compile(MethodVisitor mv) {
                    }
                }, new BytecodeExpr(right, right.getType()){
                    protected void compile(MethodVisitor mv) {
                    }
                }));
                exp.setSourcePosition(parent);

                final CastExpression cast = new CastExpression(getType(), right);
                cast.setSourcePosition(right);
                final BytecodeExpr finalRight = (BytecodeExpr) compiler.transform(cast);

                final BytecodeExpr delegate = (BytecodeExpr) compiler.transform(exp);
                return new BytecodeExpr(parent, getType()) {
                    protected void compile(MethodVisitor mv) {
                        object.compile(mv);
                        mv.visitLdcInsn(propName);
                        finalRight.compile(mv);
                        dup_x2(finalRight.getType(), mv);
                        delegate.compile(mv);
                        mv.visitInsn(POP); // remove ACONST_NULL added by delegate
                    }
                };
            }
        }
        return null;
    }

    public BytecodeExpr createBinopAssign(ASTNode parent, Token method, BytecodeExpr right, CompilerTransformer compiler) {
        throw new UnsupportedOperationException();
    }

    protected void compile(MethodVisitor mv) {
        delegate.compile(mv);
    }
}
