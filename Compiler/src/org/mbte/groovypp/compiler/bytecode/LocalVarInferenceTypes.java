/*
 * Copyright 2009-2011 MBTE Sweden AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mbte.groovypp.compiler.bytecode;

import groovypp.concurrent.FHashMap;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.Variable;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.mbte.groovypp.compiler.TypeUtil;
import org.mbte.groovypp.compiler.bytecode.BytecodeLabelInfo;

import java.util.IdentityHashMap;
import java.util.Map;

public class LocalVarInferenceTypes extends BytecodeLabelInfo {

    private FHashMap<Variable, ClassNode> defVars;
    private boolean visited;
    LocalVarInferenceTypes parentScopeInference;

    VariableExpression instanceOfVar;
    ClassNode          instanceOfType;
    int instanceOfIndex;

    // Return false if the inference is illegal. Currently this only happens if the var is reassigned to incompatible
    // type inside the loop.
    public boolean add(VariableExpression ve, ClassNode type) {
        if (defVars == null)
            defVars = FHashMap.emptyMap;

        if (ve.getAccessedVariable() != null) {
            defVars = defVars.put(ve.getAccessedVariable(), type);
//            dumpMap(ve);
        }
        else {
            boolean done = false;
            for (Map.Entry<Variable,ClassNode> variable : defVars.entrySet()) {
                if (variable.getKey().getName().equals(ve.getName())) {
                    defVars = defVars.put(variable.getKey(), type);
//                    dumpMap(ve);
                    done = true;
                    break;
                }
            }

            if (!done) {
                defVars = defVars.put(ve, type);
//                dumpMap(ve);
            }
        }

        if (parentScopeInference != null && parentScopeInference.defVars != null) {
            final ClassNode oldType = parentScopeInference.defVars.get(ve.getAccessedVariable());
            if (oldType != null) {
                if (!TypeUtil.isDirectlyAssignableFrom(oldType, type)) {
                    return false;
                }
            }
        }

        return true;
    }

    private void dumpMap(VariableExpression ve) {
        System.out.print(ve.getName());
        System.out.print("  ");
        System.out.print(defVars.size());
        System.out.print("  ");
        for (Map.Entry<Variable,ClassNode> variable : defVars.entrySet()) {
            System.out.print(variable.getKey().getName());
            System.out.print("->");
            System.out.print(variable.getValue().getNameWithoutPackage());
            System.out.print(",");
        }
        System.out.println();
    }

    public ClassNode get(VariableExpression ve) {
        if (defVars == null)
            return ClassHelper.OBJECT_TYPE;
        else {
            final Variable accessed = ve.getAccessedVariable();
            if (accessed != null)
                return defVars.get(accessed);
            else {
                for (Map.Entry<Variable,ClassNode> variable : defVars.entrySet()) {
                    if (variable.getKey().getName().equals(ve.getName())) {
                        return variable.getValue();
                    }
                }

                return null;
            }
        }
    }

    public ClassNode get(Variable ve) {
        return defVars == null ? ClassHelper.OBJECT_TYPE : defVars.get(ve);
    }


    public void jumpFrom(LocalVarInferenceTypes cur) {
        if (!visited) {
            if (defVars == null) {
                // we are 1st time here - just init
                if (cur.defVars != null)
                    defVars = cur.defVars;
                else
                    defVars = FHashMap.emptyMap;
            } else {
                // we were here already, so we need to merge
                if (cur.defVars != null)
                    for (Map.Entry<Variable, ClassNode> e : cur.defVars.entrySet()) {
                        final ClassNode oldType = defVars.get(e.getKey());
                        if (oldType != null) {
                            final ClassNode newType = TypeUtil.commonType(oldType, e.getValue());
                            if(newType != oldType)
                                defVars = defVars.put(e.getKey(), newType);
                        }
                    }
            }
        } else {
            // jump back - all is checked, nothing too be done at this point.
        }
    }

    public void comeFrom(LocalVarInferenceTypes cur) {
        jumpFrom(cur);
        if(instanceOfVar != null) {
            add(instanceOfVar, instanceOfType);
        }
        visited = true;
    }

    public void bringType(VariableExpression ve, ClassNode type, int index) {
        if(!visited) {
            instanceOfType = type;
            instanceOfVar = ve;
            instanceOfIndex = index;
        }
    }
}
