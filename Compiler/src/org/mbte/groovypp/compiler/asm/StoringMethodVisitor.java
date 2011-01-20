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

import org.objectweb.asm.*;

import java.util.*;

public class StoringMethodVisitor implements MethodVisitor {
    public final List<AsmInstr> operations = new LinkedList<AsmInstr>();

    public void visitLabel(Label label) {
        operations.add(new VisitLabel(label));
    }

    public void visitJumpInsn(int opcode, Label label) {
        operations.add(new VisitJumpInsn(opcode, label));
    }

    public void visitInsn(int opcode) {
        operations.add(new VisitInsn(opcode));
    }

    public void visitIntInsn(int opcode, int operand) {
        operations.add(new VisitIntInsn(opcode, operand));
    }

    public void visitVarInsn(int opcode, int var) {
        operations.add(new VisitVarInsn(opcode, var));
    }

    public void visitTypeInsn(int opcode, String type) {
        operations.add(new VisitTypeInsn(opcode, type));
    }

    public void visitFieldInsn(int opcode, String owner, String name, String type) {
        operations.add(new VisitFieldInsn(opcode, owner, name, type));
    }

    public void visitMethodInsn(int opcode, String owner, String name, String descr) {
        operations.add(new VisitMethodInsn(opcode, owner, name, descr));
    }

    public void visitLdcInsn(Object value) {
        operations.add(new VisitLdcInsn(value));
    }

    public void visitIincInsn(int var, int increment) {
        operations.add(new VisitIincInsn(var, increment));
    }

    public void visitMultiANewArrayInsn(String s, int i) {
        operations.add(new VisitMultiANewArrayInsn(s, i));
    }

    public void visitTryCatchBlock(Label label, Label label1, Label label2, String s) {
        operations.add(new VisitTryCatchBlock(label, label1, label2, s));
    }

    public void visitLocalVariable(String s, String s1, String s2, Label label, Label label1, int i) {
        operations.add(new VisitLocalVariable(s, s1, label, label1, i));
    }

    public void visitLineNumber(int line, Label label) {
        operations.add(new VisitLineNumber(line, label));
    }

    static class Redirects extends IdentityHashMap<Label,Label> {
        private Label redirect(Label label) {
            Label redir = get(label);
            return redir != null ? redir : label;
        }
    }

    public void redirect() {
        Redirects redirects = new Redirects();

        Label curLabel = null;
        for(Iterator<AsmInstr> i = operations.iterator(); i.hasNext(); ) {
            AsmInstr ii = i.next();

            if(ii instanceof VisitLabel) {
                VisitLabel visitLabel = (VisitLabel) ii;
                if(curLabel != null) {
                    redirects.put(visitLabel.label, curLabel);
                    i.remove();
                }
                else {
                    curLabel = visitLabel.label;
                }
            }
            else {
                curLabel = null;
            }
        }

        for(AsmInstr i : operations) {
            if(i instanceof VisitJumpInsn) {
                VisitJumpInsn jumpInsn = (VisitJumpInsn) i;
                jumpInsn.label = redirects.redirect(jumpInsn.label);
                continue;
            }

            if(i instanceof VisitLocalVariable) {
                VisitLocalVariable localVariable = (VisitLocalVariable) i;
                localVariable.start = redirects.redirect(localVariable.start);
                localVariable.end = redirects.redirect(localVariable.end);
                continue;
            }

            if(i instanceof VisitTryCatchBlock) {
                VisitTryCatchBlock tryCatchBlock = (VisitTryCatchBlock) i;
                tryCatchBlock.start = redirects.redirect(tryCatchBlock.start);
                tryCatchBlock.end = redirects.redirect(tryCatchBlock.end);
                tryCatchBlock.handler = redirects.redirect(tryCatchBlock.handler);
                continue;
            }

            if(i instanceof VisitLineNumber) {
                VisitLineNumber lineNumber = (VisitLineNumber) i;
                lineNumber.label = redirects.redirect(lineNumber.label);
                continue;
            }
        }

        redirects.clear();

        for (int i = 1; i < operations.size(); ++i) {
            AsmInstr instr = operations.get(i);
            if(instr instanceof VisitJumpInsn) {
                VisitJumpInsn jumpInsn = (VisitJumpInsn) instr;
                if(jumpInsn.opcode == Opcodes.GOTO) {
                    AsmInstr prevInstr = operations.get(i - 1);
                    if(prevInstr instanceof VisitLabel) {
                        redirects.put(((VisitLabel)prevInstr).label, jumpInsn.label);
                    }
                }
            }
        }

        if(redirects.size() > 0) {
            for (AsmInstr instr : operations) {
                if(instr instanceof VisitJumpInsn) {
                    VisitJumpInsn jumpInsn = (VisitJumpInsn) instr;
                    jumpInsn.label = redirects.redirect(jumpInsn.label);
                }
            }
        }
    }

    public void visitTableSwitchInsn(int i, int i1, Label label, Label[] labels) {
        throw new UnsupportedOperationException();
    }

    public void visitLookupSwitchInsn(Label label, int[] ints, Label[] labels) {
        throw new UnsupportedOperationException();
    }

    public void visitMaxs(int i, int i1) {
        throw new UnsupportedOperationException();
    }

    public void visitEnd() {
        throw new UnsupportedOperationException();
    }

    public AnnotationVisitor visitAnnotationDefault() {
        throw new UnsupportedOperationException();
    }

    public AnnotationVisitor visitAnnotation(String s, boolean b) {
        throw new UnsupportedOperationException();
    }

    public AnnotationVisitor visitParameterAnnotation(int i, String s, boolean b) {
        throw new UnsupportedOperationException();
    }

    public void visitAttribute(Attribute attribute) {
        throw new UnsupportedOperationException();
    }

    public void visitCode() {
        throw new UnsupportedOperationException();
    }

    public void visitFrame(int i, int i1, Object[] objects, int i2, Object[] objects1) {
        throw new UnsupportedOperationException();
    }
}
