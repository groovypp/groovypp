/*
 * Copyright 2003-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mbte.groovypp.compiler;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.classgen.ClassGeneratorException;
import org.codehaus.groovy.reflection.ReflectionCache;
import org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * A helper class for bytecode generation with AsmClassGenerator.
 *
 * @author <a href="mailto:james@coredevelopers.net">James Strachan</a>
 * @author <a href="mailto:b55r@sina.com">Bing Ran</a>
 * @author <a href="mailto:blackdrag@gmx.org">Jochen Theodorou</a>
 * @author Alex Tkachman
 */
public class BytecodeHelper implements Opcodes {

    private MethodVisitor mv;

    public BytecodeHelper(MethodVisitor mv) {
        this.mv = mv;
    }

    /**
     * Generates the bytecode to autobox the current value on the stack
     */
    public void box(Class type) {
        if (ReflectionCache.getCachedClass(type).isPrimitive && type != void.class) {
            String returnString = "(" + getTypeDescription(type) + ")Ljava/lang/Object;";
            mv.visitMethodInsn(INVOKESTATIC, getClassInternalName(DefaultTypeTransformation.class.getName()), "box", returnString);
        }
    }

    public void box(ClassNode type) {
        if (type.isPrimaryClassNode()) return;
        box(type.getTypeClass());
    }

    /**
     * Generates the bytecode to unbox the current value on the stack
     */
    public void unbox(Class type) {
        if (type.isPrimitive() && type != Void.TYPE) {
            String returnString = "(Ljava/lang/Object;)" + getTypeDescription(type);
            mv.visitMethodInsn(
                    INVOKESTATIC,
                    getClassInternalName(DefaultTypeTransformation.class.getName()),
                    type.getName() + "Unbox",
                    returnString);
        }
    }

    public void unbox(ClassNode type) {
        if (type.isPrimaryClassNode()) return;
        unbox(type.getTypeClass());
    }

    public static String getClassInternalName(ClassNode t) {
        if (t.isPrimaryClassNode()) {
            return getClassInternalName(t.getName());
        }
        return getClassInternalName(t.getTypeClass());
    }

    public static String getClassInternalName(Class t) {
        return org.objectweb.asm.Type.getInternalName(t);
    }

    /**
     * @return the ASM internal name of the type
     */
    public static String getClassInternalName(String name) {
        return name.replace('.', '/');
    }

    /**
     * @return the ASM method type descriptor
     */
    public static String getMethodDescriptor(ClassNode returnType, Parameter[] parameters) {
        StringBuffer buffer = new StringBuffer("(");
        for (int i = 0; i < parameters.length; i++) {
            buffer.append(getTypeDescription(parameters[i].getType()));
        }
        buffer.append(")");
        buffer.append(getTypeDescription(returnType));
        return buffer.toString();
    }

    public static String getTypeDescription(Class c) {
        return org.objectweb.asm.Type.getDescriptor(c);
    }

    /**
     * array types are special:
     * eg.: String[]: classname: [Ljava.lang.String;
     * Object:   classname: java.lang.Object
     * int[] :   classname: [I
     * unlike getTypeDescription '.' is not replaced by '/'.
     * it seems that makes problems for
     * the class loading if '.' is replaced by '/'
     *
     * @return the ASM type description for class loading
     */
    public static String getClassLoadingTypeDescription(ClassNode c) {
        StringBuffer buf = new StringBuffer();
        boolean array = false;
        while (true) {
            if (c.isArray()) {
                buf.append('[');
                c = c.getComponentType();
                array = true;
            } else {
                if (ClassHelper.isPrimitiveType(c)) {
                    buf.append(getTypeDescription(c));
                } else {
                    if (array) buf.append('L');
                    buf.append(c.getName());
                    if (array) buf.append(';');
                }
                return buf.toString();
            }
        }
    }

    /**
     * array types are special:
     * eg.: String[]: classname: [Ljava/lang/String;
     * int[]: [I
     *
     * @return the ASM type description
     */
    public static String getTypeDescription(ClassNode c) {
        return getTypeDescription(c, true);
    }

    /**
     * array types are special:
     * eg.: String[]: classname: [Ljava/lang/String;
     * int[]: [I
     *
     * @return the ASM type description
     */
    private static String getTypeDescription(ClassNode c, boolean end) {
        StringBuffer buf = new StringBuffer();
        ClassNode d = c;
        while (true) {
            if (ClassHelper.isPrimitiveType(d)) {
                char car;
                if (d == ClassHelper.int_TYPE) {
                    car = 'I';
                } else if (d == ClassHelper.VOID_TYPE) {
                    car = 'V';
                } else if (d == ClassHelper.boolean_TYPE) {
                    car = 'Z';
                } else if (d == ClassHelper.byte_TYPE) {
                    car = 'B';
                } else if (d == ClassHelper.char_TYPE) {
                    car = 'C';
                } else if (d == ClassHelper.short_TYPE) {
                    car = 'S';
                } else if (d == ClassHelper.double_TYPE) {
                    car = 'D';
                } else if (d == ClassHelper.float_TYPE) {
                    car = 'F';
                } else /* long */ {
                    car = 'J';
                }
                buf.append(car);
                return buf.toString();
            } else if (d.isArray()) {
                buf.append('[');
                d = d.getComponentType();
            } else {
                buf.append('L');
                String name = d.getName();
                int len = name.length();
                for (int i = 0; i < len; ++i) {
                    char car = name.charAt(i);
                    buf.append(car == '.' ? '/' : car);
                }
                if (end) buf.append(';');
                return buf.toString();
            }
        }
    }

    public void load(ClassNode type, int idx) {
        if (type == ClassHelper.double_TYPE) {
            mv.visitVarInsn(DLOAD, idx);
        } else if (type == ClassHelper.float_TYPE) {
            mv.visitVarInsn(FLOAD, idx);
        } else if (type == ClassHelper.long_TYPE) {
            mv.visitVarInsn(LLOAD, idx);
        } else if (
                type == ClassHelper.boolean_TYPE
                        || type == ClassHelper.char_TYPE
                        || type == ClassHelper.byte_TYPE
                        || type == ClassHelper.int_TYPE
                        || type == ClassHelper.short_TYPE) {
            mv.visitVarInsn(ILOAD, idx);
        } else {
            mv.visitVarInsn(ALOAD, idx);
        }
    }


    public static ClassNode boxOnPrimitive(ClassNode type) {
        if (!type.isArray()) return ClassHelper.getWrapper(type);
        return boxOnPrimitive(type.getComponentType()).makeArray();
    }

    public void dup() {
        mv.visitInsn(DUP);
    }

    private static boolean hasGenerics(Parameter[] param) {
        if (param.length == 0) return false;
        for (int i = 0; i < param.length; i++) {
            ClassNode type = param[i].getType();
            if (hasGenerics(type)) return true;
        }
        return false;
    }

    private static boolean hasGenerics(ClassNode type) {
        return type.isArray() ? hasGenerics(type.getComponentType()) : type.getGenericsTypes() != null;
    }

    public static String getGenericsMethodSignature(MethodNode node) {
        GenericsType[] generics = node.getGenericsTypes();
        Parameter[] param = node.getParameters();
        ClassNode returnType = node.getReturnType();

        if (generics == null && !hasGenerics(param) && !hasGenerics(returnType)) return null;

        StringBuffer ret = new StringBuffer(100);
        getGenericsTypeSpec(ret, generics);

        GenericsType[] paramTypes = new GenericsType[param.length];
        for (int i = 0; i < param.length; i++) {
            ClassNode pType = param[i].getType();
            if (pType.getGenericsTypes() == null || !pType.isGenericsPlaceHolder()) {
                paramTypes[i] = new GenericsType(pType);
            } else {
                paramTypes[i] = pType.getGenericsTypes()[0];
            }
        }
        addSubTypes(ret, paramTypes, "(", ")");
        addSubTypes(ret, new GenericsType[]{new GenericsType(returnType)}, "", "");
        return ret.toString();
    }

    private static void getGenericsTypeSpec(StringBuffer ret, GenericsType[] genericsTypes) {
        if (genericsTypes == null) return;
        ret.append('<');
        for (int i = 0; i < genericsTypes.length; i++) {
            String name = genericsTypes[i].getName();
            ret.append(name);
            ret.append(':');
            writeGenericsBounds(ret, genericsTypes[i], true);
        }
        ret.append('>');
    }

    private static void writeGenericsBoundType(StringBuffer ret, ClassNode printType, boolean writeInterfaceMarker) {
        if (writeInterfaceMarker && printType.isInterface()) ret.append(":");
        if (printType.equals(ClassHelper.OBJECT_TYPE) && printType.getGenericsTypes() != null) {
            ret.append("T");
            ret.append(printType.getGenericsTypes()[0].getName());
            ret.append(";");
        }
        else {
            ret.append(getTypeDescription(printType, false));
            addSubTypes(ret, printType.getGenericsTypes(), "<", ">");
            if (!ClassHelper.isPrimitiveType(printType)) ret.append(";");
        }
    }

    private static void writeGenericsBounds(StringBuffer ret, GenericsType type, boolean writeInterfaceMarker) {
        if (type.getUpperBounds() != null) {
            ClassNode[] bounds = type.getUpperBounds();
            for (int i = 0; i < bounds.length; i++) {
                writeGenericsBoundType(ret, bounds[i], writeInterfaceMarker);
            }
        } else if (type.getLowerBound() != null) {
            writeGenericsBoundType(ret, type.getLowerBound(), writeInterfaceMarker);
        } else {
            writeGenericsBoundType(ret, type.getType(), writeInterfaceMarker);
        }
    }

    private static void addSubTypes(StringBuffer ret, GenericsType[] types, String start, String end) {
        if (types == null) return;
        ret.append(start);
        for (int i = 0; i < types.length; i++) {
            if (types[i].getType().isArray()) {
                ret.append("[");
                addSubTypes(ret, new GenericsType[]{new GenericsType(types[i].getType().getComponentType())}, "", "");
            }
            else {
                if (types[i].isPlaceholder()) {
                    ret.append('T');
                    String name = types[i].getName();
                    ret.append(name);
                    ret.append(';');
                } else if (types[i].isWildcard()) {
                    if (types[i].getUpperBounds() != null) {
                        ret.append('+');
                        writeGenericsBounds(ret, types[i], false);
                    } else if (types[i].getLowerBound() != null) {
                        ret.append('-');
                        writeGenericsBounds(ret, types[i], false);
                    } else {
                        ret.append('*');
                    }
                } else {
                    writeGenericsBounds(ret, types[i], false);
                }
            }
        }
        ret.append(end);
    }

}
