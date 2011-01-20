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

import groovy.lang.GroovyObject;
import groovy.lang.MetaClass;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.codehaus.groovy.classgen.BytecodeInstruction;
import org.codehaus.groovy.classgen.BytecodeSequence;
import org.codehaus.groovy.classgen.Verifier;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.reflection.ClassInfo;
import org.codehaus.groovy.transform.ASTTransformationVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

public class CleaningVerifier extends Verifier {
    public final Set<ClassNode> ignore = new HashSet<ClassNode>();

    private static final ClassNode GO = ClassHelper.make(GroovyObject.class);

    private static final Parameter[] INVOKE_METHOD_PARAMS = new Parameter[]{
            new Parameter(ClassHelper.STRING_TYPE, "method"),
            new Parameter(ClassHelper.OBJECT_TYPE, "arguments")
    };
    private static final Parameter[] SET_PROPERTY_PARAMS = new Parameter[]{
            new Parameter(ClassHelper.STRING_TYPE, "property"),
            new Parameter(ClassHelper.OBJECT_TYPE, "value")
    };
    private static final Parameter[] GET_PROPERTY_PARAMS = new Parameter[]{
            new Parameter(ClassHelper.STRING_TYPE, "property")
    };
    private static final Parameter[] SET_METACLASS_PARAMS = new Parameter[] {
            new Parameter(ClassHelper.METACLASS_TYPE, "mc")
    };

    private static Field fieldCompUnit, fieldVerifier;
    private boolean carefully;

    static {
        for(Field f : ASTTransformationVisitor.class.getDeclaredFields()) {
            if(f.getName().equals("compUnit")) {
                fieldCompUnit = f;
                break;
            }
        }

        fieldCompUnit.setAccessible(true);

        for(Field f : CompilationUnit.class.getDeclaredFields()) {
            if(f.getName().equals("verifier")) {
                fieldVerifier = f;
                break;
            }
        }
        fieldVerifier.setAccessible(true);
    }

    public static CleaningVerifier getCleaningVerifier () {
        try {
            CompilationUnit cu = (CompilationUnit) fieldCompUnit.get(null);

            Verifier verifier = (Verifier) fieldVerifier.get(cu);
            CleaningVerifier cleaningVerifier;
            if (verifier instanceof CleaningVerifier) {
                cleaningVerifier = (CleaningVerifier) verifier;
            } else {
                fieldVerifier.set(cu, cleaningVerifier = new CleaningVerifier());
            }
            return cleaningVerifier;
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static void improveVerifier(ClassNode classNode) {
        CleaningVerifier verifier = getCleaningVerifier();
        verifier.ignore.add(classNode);
        if(!classNode.isInterface())
            verifier.addGroovyObjectInterfaceAndMethods(classNode, BytecodeHelper.getClassInternalName(classNode));
    }

    protected void addGroovyObjectInterfaceAndMethods(ClassNode node, final String classInternalName) {
        if (!node.isDerivedFromGroovyObject()) node.addInterface(GO);

        if (!node.hasMethod("getMetaClass", Parameter.EMPTY_ARRAY)) {
            addMethod(node,!Modifier.isAbstract(node.getModifiers()),
                    "getMetaClass",
                    ACC_PUBLIC,
                    ClassHelper.METACLASS_TYPE,
                    Parameter.EMPTY_ARRAY,
                    ClassNode.EMPTY_ARRAY,
                    new BytecodeSequence(new BytecodeInstruction() {
                public void visit(MethodVisitor mv) {
                    mv.visitVarInsn(ALOAD, 0);
                    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;");
                    mv.visitMethodInsn(INVOKESTATIC, "org/codehaus/groovy/reflection/ClassInfo", "getClassInfo", "(Ljava/lang/Class;)Lorg/codehaus/groovy/reflection/ClassInfo;");
                    mv.visitVarInsn(ALOAD, 0);
                    mv.visitMethodInsn(INVOKEVIRTUAL, "org/codehaus/groovy/reflection/ClassInfo", "getMetaClass", "(Ljava/lang/Object;)Lgroovy/lang/MetaClass;");
                    mv.visitInsn(ARETURN);
                }
            })
            );
        }

        Parameter[] parameters = new Parameter[] { new Parameter(ClassHelper.METACLASS_TYPE, "mc") };
        if (!node.hasMethod("setMetaClass", parameters)) {
            List list = new ArrayList();
            list.add (new BytecodeInstruction() {
                public void visit(MethodVisitor mv) {
                    mv.visitVarInsn(ALOAD, 0);
                    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;");
                    mv.visitMethodInsn(INVOKESTATIC, "org/codehaus/groovy/reflection/ClassInfo", "getClassInfo", "(Ljava/lang/Class;)Lorg/codehaus/groovy/reflection/ClassInfo;");
                    mv.visitVarInsn(ALOAD, 0);
                    mv.visitVarInsn(ALOAD, 1);
                    mv.visitMethodInsn(INVOKEVIRTUAL, "org/codehaus/groovy/reflection/ClassInfo", "setPerInstanceMetaClass", "(Ljava/lang/Object;Lgroovy/lang/MetaClass;)V");
                    mv.visitInsn(RETURN);
                }
            });
            BytecodeSequence setMetaClassCode = new BytecodeSequence(list);

            addMethod(node,!Modifier.isAbstract(node.getModifiers()),
                    "setMetaClass",
                    ACC_PUBLIC,
                    ClassHelper.VOID_TYPE,
                    SET_METACLASS_PARAMS,
                    ClassNode.EMPTY_ARRAY,
                    setMetaClassCode
            );
        }
        if (!node.hasMethod("invokeMethod",INVOKE_METHOD_PARAMS)) {
            VariableExpression vMethods = new VariableExpression("method");
            VariableExpression vArguments = new VariableExpression("arguments");
            VariableScope blockScope = new VariableScope();
            blockScope.putReferencedLocalVariable(vMethods);
            blockScope.putReferencedLocalVariable(vArguments);

            addMethod(node,!Modifier.isAbstract(node.getModifiers()),
                    "invokeMethod",
                    ACC_PUBLIC,
                    ClassHelper.OBJECT_TYPE, INVOKE_METHOD_PARAMS,
                    ClassNode.EMPTY_ARRAY,
                    new BytecodeSequence(new BytecodeInstruction() {
                public void visit(MethodVisitor mv) {
                    mv.visitVarInsn(ALOAD, 0);
                    mv.visitMethodInsn(INVOKEVIRTUAL, classInternalName, "getMetaClass", "()Lgroovy/lang/MetaClass;");
                    mv.visitVarInsn(ALOAD, 0);
                    mv.visitVarInsn(ALOAD, 1);
                    mv.visitVarInsn(ALOAD, 2);
                    mv.visitMethodInsn(INVOKEINTERFACE, "groovy/lang/MetaClass", "invokeMethod", "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/Object;");
                    mv.visitInsn(ARETURN);
                }
            })
            );
        }

        if (!node.hasMethod("getProperty", GET_PROPERTY_PARAMS)) {
            addMethod(node,!Modifier.isAbstract(node.getModifiers()),
                    "getProperty",
                    ACC_PUBLIC,
                    ClassHelper.OBJECT_TYPE,
                    GET_PROPERTY_PARAMS,
                    ClassNode.EMPTY_ARRAY,
                    new BytecodeSequence(new BytecodeInstruction() {
                public void visit(MethodVisitor mv) {
                    mv.visitVarInsn(ALOAD, 0);
                    mv.visitMethodInsn(INVOKEVIRTUAL, classInternalName, "getMetaClass", "()Lgroovy/lang/MetaClass;");
                    mv.visitVarInsn(ALOAD, 0);
                    mv.visitVarInsn(ALOAD, 1);
                    mv.visitMethodInsn(INVOKEINTERFACE, "groovy/lang/MetaClass", "getProperty", "(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;");
                    mv.visitInsn(ARETURN);
                }
            })
            );
        }

        if (!node.hasMethod("setProperty", SET_PROPERTY_PARAMS)) {
            addMethod(node,!Modifier.isAbstract(node.getModifiers()),
                    "setProperty",
                    ACC_PUBLIC,
                    ClassHelper.VOID_TYPE,
                    SET_PROPERTY_PARAMS,
                    ClassNode.EMPTY_ARRAY,
                    new BytecodeSequence(new BytecodeInstruction() {
                public void visit(MethodVisitor mv) {
                    mv.visitVarInsn(ALOAD, 0);
                    mv.visitMethodInsn(INVOKEVIRTUAL, classInternalName, "getMetaClass", "()Lgroovy/lang/MetaClass;");
                    mv.visitVarInsn(ALOAD, 0);
                    mv.visitVarInsn(ALOAD, 1);
                    mv.visitVarInsn(ALOAD, 2);
                    mv.visitMethodInsn(INVOKEINTERFACE, "groovy/lang/MetaClass", "setProperty", "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)V");
                    mv.visitInsn(RETURN);                    }
            })
            );
        }
    }

    public void addInitialization(ClassNode node) {
        if(ignore.contains(node) || Modifier.isAbstract(node.getModifiers())) {
            FieldNode field = node.getDeclaredField("metaClass");
            if(field != null && (field.getModifiers() & ACC_SYNTHETIC) != 0) {
                field.setInitialValueExpression(null);
            }
        }
        super.addInitialization(node);
    }

    public void visitClass(ClassNode node) {
        super.visitClass(node);

        for (FieldNode fieldNode : node.getFields()) {
            fieldNode.setInitialValueExpression(null);
        }

        if(ignore.contains(node) || Modifier.isAbstract(node.getModifiers())) {
            FieldNode field = node.getDeclaredField("metaClass");
            if(field != null && (field.getModifiers() & ACC_SYNTHETIC) != 0 || ignore.contains(node.getSuperClass())) {
//                System.out.println("Cleaning " + node);

                ClassNode[] interfaces = node.getInterfaces();
                for(ClassNode c : interfaces) {
                    if(c.equals(GO)) {
                        ArrayList<ClassNode> nl = new ArrayList<ClassNode>();
                        for(ClassNode cc : interfaces) {
                            if(!cc.equals(GO)) {
                                nl.add(cc);
                            }
                        }
                        node.setInterfaces(nl.toArray(new ClassNode[interfaces.length-1]));
                        break;
                    }
                }
            }

            node.removeField("metaClass");

            removeMethod(node, "$getStaticMetaClass");
        }
    }

    private void removeMethod(ClassNode node, String name) {
        for(Iterator<MethodNode> it = node.getMethods().iterator(); it.hasNext(); ) {
            MethodNode methodNode = it.next();
            if(((methodNode.getModifiers() & ACC_SYNTHETIC) != 0 || Modifier.isAbstract(node.getModifiers())) && methodNode.getName().equals(name)) {
//                System.out.println(methodNode);
                it.remove();
            }
        }
    }

    protected void addPropertyMethod(MethodNode method) {
    	super.addPropertyMethod(method);
        ClassNodeCache.clearCache(method.getDeclaringClass());
    }

    public void visitProperty(PropertyNode node) {
        super.visitProperty(node);
        node.setGetterBlock(null);
        node.setSetterBlock(null);
    }

    public void addInitialization(ClassNode node, ConstructorNode constructorNode) {
        if (constructorNode.getCode() instanceof BytecodeSequence)
            return;

        if(carefully) {
            FieldNode field = node.getDeclaredField("metaClass");
            if(field != null && (field.getModifiers() & ACC_SYNTHETIC) != 0) {
                field.setInitialValueExpression(null);
            }
        }

        super.addInitialization(node, constructorNode);
    }

    public void visitClassCarefully(ClassNode classNode) {
        carefully = true;
        visitClass(classNode);
        carefully = false;
    }
}
