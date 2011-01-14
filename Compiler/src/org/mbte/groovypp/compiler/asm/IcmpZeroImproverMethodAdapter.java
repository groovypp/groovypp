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

import org.mbte.groovypp.compiler.bytecode.StackAwareMethodAdapter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class IcmpZeroImproverMethodAdapter extends StackAwareMethodAdapter {
    private boolean load;

    public IcmpZeroImproverMethodAdapter(MethodVisitor mv) {
        super(mv);
    }

    private void dropLoad() {
        if (load) {
            super.visitInsn(I2L);
            load = false;
        }
    }

    public void visitInsn(int opcode) {
        if(opcode == I2L) {
            if(load) {
                super.visitInsn(I2L);
            }
            else {
                load = true;
            }
        }
        else {
            if(opcode == L2I) {
                if(load) {
                    load = false;
                }
                else {
                    super.visitInsn(L2I);
                }
            }
            else {
                dropLoad();
                super.visitInsn(opcode);
            }
        }
    }

    public void visitIntInsn(int opcode, int operand) {
        dropLoad();
        super.visitIntInsn(opcode, operand);
    }

    public void visitVarInsn(int opcode, int var) {
        dropLoad();
        super.visitVarInsn(opcode, var);
    }

    public void visitTypeInsn(int opcode, String desc) {
        dropLoad();
        super.visitTypeInsn(opcode, desc);
    }

    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        dropLoad();
        super.visitFieldInsn(opcode, owner, name, desc);
    }

    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
        dropLoad();
        super.visitMethodInsn(opcode, owner, name, desc);
    }

    public void visitJumpInsn(int opcode, Label label) {
        dropLoad();
        super.visitJumpInsn(opcode, label);
    }

    public void visitLabel(Label label) {
        dropLoad();
        super.visitLabel(label);
    }

    public void visitLdcInsn(Object cst) {
        dropLoad();
        super.visitLdcInsn(cst);
    }

    public void visitIincInsn(int var, int increment) {
        dropLoad();
        super.visitIincInsn(var, increment);
    }

    public void visitTableSwitchInsn(int min, int max, Label dflt, Label labels[]) {
        dropLoad();
        super.visitTableSwitchInsn(min, max, dflt, labels);
    }

    public void visitLookupSwitchInsn(Label dflt, int keys[], Label labels[]) {
        dropLoad();
        super.visitLookupSwitchInsn(dflt, keys, labels);
    }

    public void visitMultiANewArrayInsn(String desc, int dims) {
        dropLoad();
        super.visitMultiANewArrayInsn(desc, dims);
    }

    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
        dropLoad();
        super.visitTryCatchBlock(start, end, handler, type);
    }
}