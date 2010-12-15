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

import groovy.lang.TypePolicy;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.InnerClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.expr.*;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.PresentationUtil;
import org.mbte.groovypp.compiler.TypeUtil;
import org.mbte.groovypp.compiler.bytecode.*;
import org.mbte.groovypp.compiler.transformers.ExprTransformer;
import org.objectweb.asm.Opcodes;

import java.text.MessageFormat;

public class AttributeExpressionTransformer extends ExprTransformer<AttributeExpression> {
    @Override
    public Expression transform(AttributeExpression exp, CompilerTransformer compiler) {
        Expression objectExpr = exp.getObjectExpression();

        BytecodeExpr obj;
        final FieldNode field;
        String propName = exp.getPropertyAsString();
        if (objectExpr instanceof ClassExpression) {
            obj = null;
            field = compiler.findField(objectExpr.getType(), propName);
            if (field == null) {
              compiler.addError("Cannot find field " + propName + " of class " + PresentationUtil.getText(objectExpr.getType()), exp);
            }
        } else {
            obj = (BytecodeExpr) compiler.transform(objectExpr);
            if(exp.getObjectExpression() instanceof VariableExpression &&
                    ((VariableExpression) exp.getObjectExpression()).getName().equals("this") &&
                    compiler.classNode instanceof InnerClassNode) {

                BytecodeExpr object;

                ClassNode thisType = compiler.classNode;
                while (thisType != null) {
                    FieldNode prop = compiler.findField(thisType, propName);
                    if (prop != null) {
                        boolean isStatic = PropertyUtil.isStatic(prop);
                        if (!isStatic && exp.isStatic()) return null;
                        object = isStatic ? null : new InnerThisBytecodeExpr(exp, thisType, compiler);

                        return new ResolvedFieldBytecodeExpr(exp, prop, object, null, compiler, true);
                    }

                    thisType = thisType.getOuterClass();
                }

                if (compiler.policy == TypePolicy.STATIC) {
                    compiler.addError(MessageFormat.format("Cannot find field {0}.{1}",
                            PresentationUtil.getText(compiler.classNode),
                            propName), exp);
                    return null;
                }
                else {
                    object = new InnerThisBytecodeExpr(exp, compiler.classNode, compiler);
                    return new UnresolvedLeftExpr(exp, null, object, propName);
                }

            }
            else {
                field = compiler.findField(obj.getType(), propName);
                if (field == null) {
                  compiler.addError("Cannot find field " + propName + " of class " + PresentationUtil.getText(obj.getType()), exp);
                }
            }
        }

        return new ResolvedFieldBytecodeExpr(exp, field, obj, null, compiler, true);
    }
}
