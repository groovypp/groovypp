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

package org.mbte.groovypp.compiler.asm;

import org.mbte.groovypp.compiler.CompilerStack;
import org.mbte.groovypp.compiler.bytecode.StackAwareMethodAdapter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class I2LL2IRemoverMethodAdapter extends IcmpZeroImproverMethodAdapter {
    private boolean load;

    public I2LL2IRemoverMethodAdapter(MethodVisitor mv) {
        super(mv);
    }

    private void dropLoad() {
        if (load) {
            super.visitInsn(ICONST_0);
            load = false;
        }
    }

    public void visitInsn(int opcode) {
        if(opcode == ICONST_0) {
            if(load) {
                super.visitInsn(ICONST_0);
            }
            else {
                load = true;
            }
        }
        else {
            dropLoad();
            super.visitInsn(opcode);
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
        if(load) {
            switch (opcode) {
                case IF_ICMPEQ:
                    super.visitJumpInsn(IFEQ, label);
                    load = false;
                    return;

                case IF_ICMPNE:
                    super.visitJumpInsn(IFNE, label);
                    load = false;
                    return;

                case IF_ICMPGE:
                    super.visitJumpInsn(IFGE, label);
                    load = false;
                    return;

                case IF_ICMPGT:
                    super.visitJumpInsn(IFGT, label);
                    load = false;
                    return;

                case IF_ICMPLE:
                    super.visitJumpInsn(IFLE, label);
                    load = false;
                    return;

                case IF_ICMPLT:
                    super.visitJumpInsn(IFLT, label);
                    load = false;
                    return;
            }

            dropLoad();
        }

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