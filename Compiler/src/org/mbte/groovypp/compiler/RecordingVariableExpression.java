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

package org.mbte.groovypp.compiler;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.expr.VariableExpression;

public class RecordingVariableExpression extends VariableExpression {
    private final VariableExpression recorder;

    private final int column;

    public RecordingVariableExpression(VariableExpression owner, VariableExpression recorder, int column) {
        super(owner.getName(), owner.getType());
        this.recorder = recorder;
        this.column = column;
        setAccessedVariable(owner.getAccessedVariable());
        setSourcePosition(owner);
    }

    public int getColumn() {
        return column;
    }

    public VariableExpression getRecorder() {
        return recorder;
    }
}
