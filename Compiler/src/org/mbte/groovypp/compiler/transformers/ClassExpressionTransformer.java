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

package org.mbte.groovypp.compiler.transformers;

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.classgen.BytecodeExpression;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;
import org.mbte.groovypp.compiler.BytecodeHelper;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.TypeUtil;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.objectweb.asm.MethodVisitor;

import java.util.LinkedList;

public class ClassExpressionTransformer extends ExprTransformer<ClassExpression> {
    public Expression transform(ClassExpression exp, CompilerTransformer compiler) {

        final ClassNode type = exp.getType();
        return new ClassExpr(exp, type, compiler);
    }

    public static BytecodeExpr newExpr(Expression exp, ClassNode type, CompilerTransformer compiler) {
        return new ClassExpr(exp, type, compiler);
    }

    private static String makeFieldClassName(ClassNode type) {
        String internalName = BytecodeHelper.getClassInternalName(type);
        StringBuffer ret = new StringBuffer(internalName.length());
        for (int i = 0; i < internalName.length(); i++) {
            char c = internalName.charAt(i);
            if (c == '/') {
                ret.append('$');
            } else if (c == ';') {
                //append nothing -> delete ';'
            } else {
                ret.append(c);
            }
        }
        return ret.toString();
    }

    private static String getStaticFieldName(ClassNode type) {
        ClassNode componentType = type;
        String prefix = "";
        for (; componentType.isArray(); componentType = componentType.getComponentType()) {
            prefix += "$";
        }
        if (prefix.length() != 0) prefix = "array" + prefix;
        return prefix + "$_class_$" + makeFieldClassName(componentType);
    }

    private static class ClassExpr extends BytecodeExpr {
        private final ClassNode type;
        private final CompilerTransformer compiler;

        public ClassExpr(Expression exp, ClassNode type, CompilerTransformer compiler) {
            super(exp, TypeUtil.withGenericTypes(ClassHelper.CLASS_Type, type));
            this.type = type;
            this.compiler = compiler;
        }

        protected void compile(MethodVisitor mv) {
            if (ClassHelper.isPrimitiveType(type)) {
                mv.visitFieldInsn(GETSTATIC, BytecodeHelper.getClassInternalName(TypeUtil.wrapSafely(type)), "TYPE", "Ljava/lang/Class;");
            } else {
                if(compiler.classNode.isInterface() || compiler.methodNode.getName().equals("<clinit>")) {
                    mv.visitLdcInsn(BytecodeHelper.getClassLoadingTypeDescription(type));
                    mv.visitMethodInsn(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;");
                }
                else {
                    final String staticFieldName = getStaticFieldName(type);
                    if(compiler.classNode.getField(staticFieldName) == null) {
                        final BytecodeExpression class$ = new BytecodeExpression() {
                            public void visit(MethodVisitor mv) {
                                mv.visitLdcInsn(BytecodeHelper.getClassLoadingTypeDescription(type));
                                mv.visitMethodInsn(INVOKESTATIC, BytecodeHelper.getClassInternalName(compiler.classNode), "class$", "(Ljava/lang/String;)Ljava/lang/Class;");
                                mv.visitInsn(DUP);
                                mv.visitFieldInsn(PUTSTATIC, BytecodeHelper.getClassInternalName(compiler.classNode), staticFieldName, "Ljava/lang/Class;");
                            }
                        };
                        compiler.classNode.addField(staticFieldName, ACC_PUBLIC | ACC_STATIC | ACC_SYNTHETIC, TypeUtil.withGenericTypes(ClassHelper.CLASS_Type,type), null);
                        final ExpressionStatement expressionStatement = new ExpressionStatement(class$);
                        final LinkedList<Statement> statements = new LinkedList<Statement>();
                        statements.add(expressionStatement);
                        compiler.classNode.addStaticInitializerStatements(statements, true);
                    }
                    mv.visitFieldInsn(GETSTATIC, BytecodeHelper.getClassInternalName(compiler.classNode), staticFieldName, "Ljava/lang/Class;");
                }
            }
        }
    }
}
