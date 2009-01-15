package org.mbte.groovypp.compiler.impl;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.util.FastArray;
import org.codehaus.groovy.reflection.ReflectionCache;
import org.codehaus.groovy.reflection.CachedMethod;
import org.codehaus.groovy.reflection.CachedClass;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.objectweb.asm.Opcodes;
import org.mbte.groovypp.runtime.DefaultGroovyPPMethods;

import java.util.*;
import java.lang.ref.SoftReference;

public class ClassNodeCache {
    public static class ClassNodeInfo {
        Map<String,Object> methods;
        Map<String,Object> fields;
        public FastArray constructors;
    }

    public static class CompileUnitInfo extends HashMap<ClassNode, ClassNodeInfo>{
    }

    static final WeakHashMap<Class, SoftReference<ClassNodeInfo>> loadedClassesCache = new WeakHashMap<Class, SoftReference<ClassNodeInfo>> ();

    static final WeakHashMap<CompileUnit, SoftReference<CompileUnitInfo>> compiledClassesCache = new WeakHashMap<CompileUnit, SoftReference<CompileUnitInfo>>();

    static final Map<ClassNode,List<MethodNode>> dgmMethods = new HashMap<ClassNode,List<MethodNode>> ();

    static {
        initDgm(DefaultGroovyMethods.class);
        initDgm(DefaultGroovyPPMethods.class);
    }

    static ClassNodeInfo getClassNodeInfo (ClassNode classNode) {
        final ModuleNode moduleNode = classNode.getModule();
        if (moduleNode != null) {
            final CompileUnit compileUnit = classNode.getCompileUnit();
            final SoftReference<CompileUnitInfo> ref = compiledClassesCache.get(compileUnit);
            CompileUnitInfo cui;
            if (ref == null || (cui = ref.get()) == null) {
                cui = new CompileUnitInfo();
                compiledClassesCache.put(compileUnit, new SoftReference<CompileUnitInfo>(cui));
            }

            ClassNodeInfo info = cui.get(classNode);
            if (info == null) {
                info = new ClassNodeInfo();
                cui.put(classNode, info);
            }
            return info;
        }
        else {
            Class typeClass = classNode.getTypeClass();
            final SoftReference<ClassNodeInfo> ref = loadedClassesCache.get(typeClass);
            ClassNodeInfo cni;
            if (ref == null || (cni = ref.get()) == null) {
                cni = new ClassNodeInfo();
                loadedClassesCache.put(typeClass, new SoftReference<ClassNodeInfo>(cni));
            }
            return cni;
        }
    }

    static synchronized FastArray getConstructors(ClassNode type) {
        final ClassNodeInfo info = getClassNodeInfo(type);

        FastArray list = info.constructors;
        if (list == null) {
            list = new FastArray ();
            info.constructors = list;

            List constructors = type.redirect().getDeclaredConstructors();
            if (constructors.isEmpty())
                list.add(new ConstructorNode(Opcodes.ACC_PUBLIC, null));
            else
                for (Object o : constructors) {
                    ConstructorNode cn = (ConstructorNode) o;
                    list.add(cn);
                }
        }
        return list;
    }

    static synchronized Object getMethods (ClassNode type, String methodName) {
        final ClassNodeInfo info = getClassNodeInfo(type);

        Map<String, Object> nameMap = info.methods;
        if (nameMap == null) {
            nameMap = new HashMap<String, Object> ();
            info.methods = nameMap;

            final Set<ClassNode> ifaces = getAllInterfaces(type);
            for (ClassNode node : ifaces) {
                addMethods(nameMap, node.getMethods(), true);
            }

            for (ClassNode cn : getSuperClasses(type)) {
                addMethods(nameMap, cn.getMethods(), cn == type);

                final List<MethodNode> list = dgmMethods.get(cn);
                if (list != null) {
                    addMethods(nameMap, list, true);
                }
            }

            for (ClassNode node : ifaces) {
                final List<MethodNode> list = dgmMethods.get(node);
                if (list != null) {
                    addMethods(nameMap, list, true);
                }
            }
        }
        return nameMap.get(methodName);
    }

    protected static LinkedList<ClassNode> getSuperClasses(ClassNode classNode) {
        LinkedList<ClassNode> superClasses = new LinkedList<ClassNode>();

        if (classNode.isInterface()) {
            superClasses.addFirst(ClassHelper.OBJECT_TYPE);
        } else {
            for (ClassNode c = classNode; c != null; c = c.getSuperClass()) {
                superClasses.addFirst(c);
            }
//            if (classNode.isArray() && !classNode.equals(ClassHelper.O) && !theClass.getComponentType().isPrimitive()) {
//                superClasses.addFirst(ReflectionCache.OBJECT_ARRAY_CLASS);
//            }
        }
        return superClasses;
    }

    static Object getFields (ClassNode type, String fieldName) {
        final ClassNodeInfo info = getClassNodeInfo(type);

        Map<String, Object> nameMap = info.fields;
        if (nameMap == null) {
            nameMap = new HashMap<String, Object>();
            info.fields = nameMap;

            for (ClassNode node : ClassNodeCache.getAllInterfaces(type)) {
                addFields(nameMap, node);
            }

            while (type != null) {
                addFields(nameMap, type);
                type = type.getSuperClass();
            }
        }
        return nameMap.get(fieldName);
    }

    private static void addFields(Map<String, Object> nameMap, ClassNode node) {
        for (FieldNode m : node.getFields()) {
            nameMap.put(m.getName(), m);
        }
    }

    static void addMethods(Map<String, Object> nameMap, List<MethodNode> nodes, boolean usePrivate) {
        for (MethodNode m : nodes) {
            if (usePrivate || !m.isPrivate()) {
                Object list = nameMap.get(m.getName());
                nameMap.put(m.getName(), addMethodToList(list, m));
            }
        }
    }

    static void getAllInterfaces(ClassNode type, Set<ClassNode> res) {
        if (type == null)
          return;

        if (type.isInterface())
          res.add(type);

        ClassNode[] interfaces = type.getInterfaces();
        for (ClassNode anInterface : interfaces) {
            res.add(anInterface);
            getAllInterfaces(anInterface, res);
        }

        getAllInterfaces(type.getSuperClass(), res);
    }

    static Set<ClassNode> getAllInterfaces(ClassNode type) {
        Set<ClassNode> res = new HashSet<ClassNode>();
        getAllInterfaces(type, res);
        return res;
    }

    public static Object addMethodToList(Object o, MethodNode method) {
        if (o == null) {
            return method;
        }

        if (o instanceof MethodNode) {
            MethodNode match = (MethodNode) o;
            if (!isMatchingMethod(match, method)) {
                FastArray list = new FastArray(2);
                list.add(match);
                list.add(method);
                return list;
            } else {
                if (match.isPrivate()
                        || (!isNonRealMethod(match) && match.getDeclaringClass().isInterface() && !method.getDeclaringClass().isInterface())) {
                    // do not overwrite interface methods with instance methods
                    // do not overwrite private methods
                    // Note: private methods from parent classes are not shown here,
                    // but when doing the multimethod connection step, we overwrite
                    // methods of the parent class with methods of a subclass and
                    // in that case we want to keep the private methods
                } else {
                    ClassNode methodC = method.getDeclaringClass();
                    ClassNode matchC = match.getDeclaringClass();
                    if (methodC.equals(matchC)) {
                        if (isNonRealMethod(method)) {
                            return method;
                        }
                    } else if (!TypeUtil.isDirectlyAssignableFrom(methodC, matchC)) {
                        return method;
                    }
                }
            }
            return o;
        }

        if (o instanceof FastArray) {
            FastArray list = (FastArray) o;
            int found = findMatchingMethod(list, method);

            if (found == -1) {
                list.add(method);
            } else {
                MethodNode match = (MethodNode) list.get(found);
                if (match.equals(method)) return o;
                if (match.isPrivate()
                        || (!isNonRealMethod(match) && match.getDeclaringClass().isInterface() && !method.getDeclaringClass().isInterface())) {
                    // do not overwrite interface methods with instance methods
                    // do not overwrite private methods
                    // Note: private methods from parent classes are not shown here,
                    // but when doing the multimethod connection step, we overwrite
                    // methods of the parent class with methods of a subclass and
                    // in that case we want to keep the private methods
                } else {
                    ClassNode methodC = method.getDeclaringClass();
                    ClassNode matchC = match.getDeclaringClass();
                    if (methodC.equals(matchC)) {
                        if (isNonRealMethod(method)) {
                            list.set(found, method);
                        }
                    } else if (!TypeUtil.isDirectlyAssignableFrom(methodC,matchC)) {
                        list.set(found, method);
                    }
                }
            }
        }

        return o;
    }

    private static boolean isNonRealMethod(MethodNode method) {
        return method instanceof DGM;
    }

    private static boolean isMatchingMethod(MethodNode aMethod, MethodNode method) {
        if (aMethod.equals(method)) return true;
        Parameter[] params1 = aMethod.getParameters();
        Parameter[] params2 = method.getParameters();
        if (params1.length != params2.length) {
            return false;
        }

        boolean matches = true;
        for (int i = 0; i < params1.length; i++) {
            if (!params1[i].getType().equals(params2[i].getType())) {
                matches = false;
                break;
            }
        }
        return matches;
    }

    private static int findMatchingMethod(FastArray list, MethodNode method) {
        int len = list.size();
        Object data[] = list.getArray();
        for (int j = 0; j != len; ++j) {
            MethodNode aMethod = (MethodNode) data[j];
            if (isMatchingMethod(aMethod, method))
                return j;
        }
        return -1;
    }

    private static void initDgm(final Class klazz) {
        final CachedMethod[] methods = ReflectionCache.getCachedClass(klazz).getMethods();
        for (int i = 0; i < methods.length; i++) {
            CachedMethod method = methods[i];
            if (method.isPublic() && method.isStatic()) {
                final CachedClass[] classes = method.getParameterTypes();
                if (classes.length > 0) {
                    ClassNode declaringClass = ClassHelper.make(method.getParameterTypes()[0].getTheClass());

                    final Class<?>[] ex = method.getCachedMethod().getExceptionTypes();
                    ClassNode exs [] = ex.length > 0 ? new ClassNode [ex.length] : ClassNode.EMPTY_ARRAY;
                    for (int j = 0; j != ex.length; ++j)
                      exs [j] = ClassHelper.make(ex[j]);

                    Parameter params [] = classes.length > 1 ? new Parameter[classes.length-1] : Parameter.EMPTY_ARRAY;
                    for (int j = 0; j != params.length; ++j)
                      params[j] = new Parameter(ClassHelper.make(classes[j+1].getTheClass()), "$"+j);

                    DGM mn = new DGM(
                            method.getName(),
                            Opcodes.ACC_PUBLIC,
                            ClassHelper.make(method.getReturnType()),
                            params,
                            exs,
                            null);
                    mn.setDeclaringClass(declaringClass);
                    mn.callClass = klazz;
                    mn.descr = BytecodeHelper.getMethodDescriptor(method.getReturnType(),method.getCachedMethod().getParameterTypes());

                    List<MethodNode> list = dgmMethods.get(declaringClass);
                    if (list == null) {
                        list = new ArrayList<MethodNode>(4);
                        dgmMethods.put(declaringClass, list);
                    }
                    list.add(mn);
                }
            }
        }
    }

    public static class DGM extends MethodNode{
        String    descr;
        Class     callClass;

        public DGM(String name, int modifiers, ClassNode returnType, Parameter[] parameters, ClassNode[] exceptions, Statement code) {
            super(name, modifiers, returnType, parameters, exceptions, code);
        }
    }
}
