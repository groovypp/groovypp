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

package org.mbte.groovypp.compiler;

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.InnerClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.expr.Expression;
import org.objectweb.asm.Opcodes;

public class UncertainClassNode<T extends Expression> extends InnerClassNode {
    protected final T expression;
    protected final MethodNode outerMethod;

    public UncertainClassNode(T expression, MethodNode owner, String name) {
        super(owner.getDeclaringClass(), name, Opcodes.ACC_PRIVATE|Opcodes.ACC_FINAL, ClassHelper.OBJECT_TYPE, ClassNode.EMPTY_ARRAY, null);
        this.expression = expression;
        setSourcePosition(expression);
        expression.setType(this);
        outerMethod = owner;
        setEnclosingMethod(outerMethod);
    }

    public T getExpression() {
        return expression;
    }

    public MethodNode getOuterMethod() {
        return outerMethod;
    }

    boolean isAssignableTo (ClassNode type) {
        return false;
    }
}
