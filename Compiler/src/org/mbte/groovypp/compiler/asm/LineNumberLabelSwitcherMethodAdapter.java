/*
 * Copyright 2009-2010 MBTE Sweden AB.
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

package org.mbte.groovypp.compiler.asm;

import org.mbte.groovypp.compiler.CompilerStack;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;

public class LineNumberLabelSwitcherMethodAdapter extends MethodAdapter {

    private int lineNumber = -239;
    private Label lineNumberLabel;

    public LineNumberLabelSwitcherMethodAdapter(MethodVisitor mv) {
        super(mv);
    }

    private void dropLineNumber() {
        if(lineNumber != -239) {
            super.visitLineNumber(lineNumber, lineNumberLabel);
            lineNumberLabel = null;
            lineNumber = -239;
        }
    }

    @Override
    public void visitLabel(Label label) {
        super.visitLabel(label);
    }

    @Override
    public void visitLineNumber(int i, Label label) {
        dropLineNumber ();
        lineNumber = i;
        lineNumberLabel = label;
    }

    @Override
    public void visitInsn(int opcode) {
        dropLineNumber ();
        super.visitInsn(opcode);
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        dropLineNumber ();
        super.visitIntInsn(opcode, operand);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        dropLineNumber ();
        super.visitVarInsn(opcode, var);
    }

    @Override
    public void visitTypeInsn(int opcode, String desc) {
        dropLineNumber ();
        super.visitTypeInsn(opcode, desc);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        dropLineNumber ();
        super.visitFieldInsn(opcode, owner, name, desc);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
        dropLineNumber ();
        super.visitMethodInsn(opcode, owner, name, desc);
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        dropLineNumber ();
        super.visitJumpInsn(opcode, label);
    }

    @Override
    public void visitLdcInsn(Object cst) {
        dropLineNumber ();
        super.visitLdcInsn(cst);
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        dropLineNumber ();
        super.visitIincInsn(var, increment);
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label[] labels) {
        dropLineNumber ();
        super.visitTableSwitchInsn(min, max, dflt, labels);
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        dropLineNumber ();
        super.visitLookupSwitchInsn(dflt, keys, labels);
    }

    @Override
    public void visitMultiANewArrayInsn(String desc, int dims) {
        dropLineNumber ();
        super.visitMultiANewArrayInsn(desc, dims);
    }

    @Override
    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
        dropLineNumber ();
        super.visitTryCatchBlock(start, end, handler, type);
    }
}