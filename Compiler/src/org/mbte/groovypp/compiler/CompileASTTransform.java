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

import groovy.lang.TypePolicy;
import groovy.lang.Typed;
import groovy.lang.Dynamic;
import groovy.lang.Mixed;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.classgen.BytecodeSequence;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.ParserPlugin;
import org.codehaus.groovy.control.ParserPluginFactory;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.messages.SyntaxErrorMessage;
import org.codehaus.groovy.syntax.SyntaxException;
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;
import org.codehaus.groovy.util.FastArray;
import org.objectweb.asm.Opcodes;

import java.util.*;

@GroovyASTTransformation(phase = CompilePhase.INSTRUCTION_SELECTION)
public class CompileASTTransform implements ASTTransformation, Opcodes {
    private static final ClassNode COMPILE_TYPE = ClassHelper.make(Typed.class);
    private static final ClassNode DYNAMIC_TYPE = ClassHelper.make(Dynamic.class);
    private static final ClassNode MIXED_TYPE   = ClassHelper.make(Mixed.class);

    public void visit(ASTNode[] nodes, final SourceUnit source) {
        if (!(nodes[0] instanceof AnnotationNode) || !(nodes[1] instanceof AnnotatedNode)) {
            throw new RuntimeException("Internal error: wrong types: $node.class / $parent.class");
        }

        AnnotatedNode parent = (AnnotatedNode) nodes[1];

        Map<MethodNode, TypePolicy> toProcess = new LinkedHashMap<MethodNode, TypePolicy>();
        final ClassNode classNode;

        if (parent instanceof MethodNode) {
            TypePolicy classPolicy = getPolicy(parent.getDeclaringClass(), source, TypePolicy.DYNAMIC);
            TypePolicy methodPolicy = getPolicy(parent, source, classPolicy);

            classNode = parent.getDeclaringClass();
            if (methodPolicy != TypePolicy.DYNAMIC) {
                final MethodNode mn = (MethodNode) parent;
                addMethodToProcessingQueue(source, toProcess, methodPolicy, mn, null);
            }

        } else if (parent instanceof ClassNode) {
            classNode = (ClassNode) parent;
            TypePolicy classPolicy = getPolicy(classNode, source, TypePolicy.DYNAMIC);

            allMethods(source, toProcess, classNode, classPolicy);
        } else if (parent instanceof PackageNode) {
            TypePolicy modulePolicy = getPolicy(parent, source, TypePolicy.DYNAMIC);
            for (ClassNode clazz : source.getAST().getClasses()) {
                if (clazz instanceof InnerClassNode) continue;

                allMethods(source, toProcess, clazz, modulePolicy);
            }
        } else {
            int line = parent.getLineNumber();
            int col = parent.getColumnNumber();
            source.getErrorCollector().addError(
                    new SyntaxErrorMessage(new SyntaxException("@Typed applicable only to classes or methods or package declaration" + '\n', line, col), source), true
            );
            return;
        }

        final Expression debugMember = ((AnnotationNode) nodes[0]).getMember("debug");
        boolean debug = debugMember != null && debugMember instanceof ConstantExpression && ((ConstantExpression) debugMember).getValue().equals(Boolean.TRUE);

        final Expression fastArraysMember = ((AnnotationNode) nodes[0]).getMember("fastArrays");
        boolean fastArrays = fastArraysMember == null || fastArraysMember instanceof ConstantExpression && !((ConstantExpression) fastArraysMember).getValue().equals(Boolean.FALSE);

        // here we want to improve method types
        for (Map.Entry<MethodNode, TypePolicy> entry : toProcess.entrySet()) {
            final MethodNode mn = entry.getKey();

            improveMethodTypes(mn);
        }

        SourceUnitContext context = new SourceUnitContext();
        for (Map.Entry<MethodNode, TypePolicy> entry : toProcess.entrySet()) {
            MethodNode mn = entry.getKey();
            final TypePolicy policy = entry.getValue();

            final List<AnnotationNode> anns = mn.getAnnotations(COMPILE_TYPE);
            boolean localDebug = debug;
            boolean localFastArrays = fastArrays;
            if (!anns.isEmpty()) {
                final AnnotationNode ann = anns.get(0);
                final Expression localDebugMember = ann.getMember("debug");
                if (localDebugMember != null)
                    localDebug = localDebugMember instanceof ConstantExpression && ((ConstantExpression) localDebugMember).getValue().equals(Boolean.TRUE);
                final Expression localFastArraysMember = ann.getMember("fastArrays");
                localFastArrays = localFastArraysMember == null || localFastArraysMember instanceof ConstantExpression && !((ConstantExpression) localFastArraysMember).getValue().equals(Boolean.FALSE);
            }

            if ((mn.getModifiers() & Opcodes.ACC_BRIDGE) != 0 || mn.isAbstract())
                continue;

            final Statement code = mn.getCode();
            if (!(code instanceof BytecodeSequence)) {
                if (!mn.getName().equals("$doCall")) {
                    String name = mn.getName().equals("<init>") ? "_init_" :
                            mn.getName().equals("<clinit>") ? "_clinit_" : mn.getName();
                    StaticMethodBytecode.replaceMethodCode(source, context, mn, new CompilerStack(null), localDebug ? 0 : -1, localFastArrays, policy, mn.getDeclaringClass().getName() + "$" + name);
                }
            }
        }

        for (MethodNode node : context.generatedFieldGetters.values()) {
            StaticMethodBytecode.replaceMethodCode(source, context, node, new CompilerStack(null), -1, true, TypePolicy.STATIC, "Neverused");
        }
        for (MethodNode node : context.generatedFieldSetters.values()) {
            StaticMethodBytecode.replaceMethodCode(source, context, node, new CompilerStack(null), -1, true, TypePolicy.STATIC, "Neverused");
        }
        for (MethodNode node : context.generatedMethodDelegates.values()) {
            StaticMethodBytecode.replaceMethodCode(source, context, node, new CompilerStack(null), -1, true, TypePolicy.STATIC, "Neverused");
        }
    }

    public static void improveMethodTypes(MethodNode mn) {
        boolean dynamic = mn.getReturnType() == ClassHelper.DYNAMIC_TYPE || mn.getReturnType() == TypeUtil.IMPROVE_TYPE;
        for(Parameter p: mn.getParameters()) {
            dynamic |= p.getType() == ClassHelper.DYNAMIC_TYPE;
        }

        if(dynamic) {
            ClassNode type = mn.getDeclaringClass();
            Object methods = ClassNodeCache.getSuperMethods(type, mn.getName());
            boolean changed = false;
            if (methods != null) {
                if (methods instanceof MethodNode) {
                    MethodNode baseMethod = (MethodNode) methods;
                    if(baseMethod.getDeclaringClass() != mn.getDeclaringClass())
                        changed = checkOverride(mn, baseMethod, type);
                }
                else {
                    FastArray methodsArr = (FastArray) methods;
                    int methodCount = methodsArr.size();
                    for (int j = 0; j != methodCount; ++j) {
                        MethodNode baseMethod = (MethodNode) methodsArr.get(j);
                        if(baseMethod.getDeclaringClass() != mn.getDeclaringClass()) {
                            changed = checkOverride(mn, baseMethod, type);
                            if(changed)
                                break;
                        }
                    }
                }
            }

            if(changed) {
                mn.addAnnotation(new AnnotationNode(TypeUtil.IMPROVED_TYPES));
                ClassNodeCache.clearCache (mn.getDeclaringClass());
            }
        }
    }

    private static boolean checkOverride(MethodNode method, MethodNode baseMethod, ClassNode baseType) {
        class Mutation {
            final int index;
            final ClassNode t;

            public Mutation(ClassNode t, int index) {
                this.t = t;
                this.index = index;
            }
        }

        List<Mutation> mutations = null;

        Parameter[] baseMethodParameters = baseMethod.getParameters();
        Parameter[] closureParameters = method.getParameters();

        if (closureParameters.length == baseMethodParameters.length) {
            for (int i = 0; i < closureParameters.length; i++) {
                Parameter closureParameter = closureParameters[i];
                Parameter missingMethodParameter = baseMethodParameters[i];

                ClassNode parameterType = missingMethodParameter.getType();
                if (!parameterType.redirect().equals(closureParameter.getType().redirect()) || closureParameter.getType() == ClassHelper.DYNAMIC_TYPE) {
                    parameterType = TypeUtil.getSubstitutedType(parameterType, baseType.redirect(), baseType);
                    if (parameterType.redirect().equals(closureParameter.getType().redirect()) ||
                        closureParameter.getType() == ClassHelper.DYNAMIC_TYPE) {
                        parameterType = TypeUtil.withGenericTypes(parameterType, (GenericsType[]) null);
                        if (mutations == null)
                            mutations = new ArrayList<Mutation>();
                        mutations.add(new Mutation(parameterType, i));
                    } else {
                        return false;
                    }
                }
            }

            if (mutations != null) {
                Parameter[] newParams = closureParameters.clone();
                for(Mutation m : mutations) {
                    newParams[m.index] = new Parameter(m.t, closureParameters[m.index].getName());
                }

                MethodNode found = method.getDeclaringClass().getDeclaredMethod(method.getName(), newParams);
                if(found != null) {
                    return false;
                }
                method.setParameters(newParams);
            }
            ClassNode returnType = TypeUtil.getSubstitutedType(baseMethod.getReturnType(), baseType.redirect(), baseType);
            method.setReturnType(returnType);
            return true;
        }

        return false;
    }

    private void addMethodToProcessingQueue(final SourceUnit source, final Map<MethodNode, TypePolicy> toProcess, final TypePolicy methodPolicy, final MethodNode mn, final LinkedList<MethodNode> methods) {
        final Statement code = mn.getCode();
        if (code == null)
            return;

        toProcess.put(mn, methodPolicy);

        code.visit(new CodeVisitorSupport(){
            public void visitConstructorCallExpression(ConstructorCallExpression call) {
                final ClassNode type = call.getType();
                if (type instanceof InnerClassNode && ((InnerClassNode)type).isAnonymous()) {
                    allMethods(source, toProcess, type, methodPolicy);
                }
                super.visitConstructorCallExpression(call);
            }
        });
    }

    private void allMethods(SourceUnit source, Map<MethodNode, TypePolicy> toProcess, ClassNode classNode, TypePolicy classPolicy) {
        LinkedList<MethodNode> methods = new LinkedList<MethodNode>(classNode.getMethods());
        while (!methods.isEmpty()) {
            MethodNode mn = methods.removeFirst();
            if (!mn.isAbstract() && (mn.getModifiers() & ACC_SYNTHETIC) == 0) {
                TypePolicy methodPolicy = getPolicy(mn, source, classPolicy);
                if (methodPolicy != TypePolicy.DYNAMIC) {
                    addMethodToProcessingQueue(source, toProcess, methodPolicy, mn, methods);
                }
            }
        }

        methods = new LinkedList<MethodNode>(classNode.getDeclaredConstructors());
        while (!methods.isEmpty()) {
            MethodNode mn = methods.removeFirst();
            TypePolicy methodPolicy = getPolicy(mn, source, classPolicy);
            if (methodPolicy != TypePolicy.DYNAMIC) {
                addMethodToProcessingQueue(source, toProcess, methodPolicy, mn, methods);
            }
        }

        if(classPolicy == TypePolicy.STATIC)
            CleaningVerifier.improveVerifier(classNode);

        Iterator<InnerClassNode> inners = classNode.getInnerClasses();
        while (inners.hasNext()) {
            InnerClassNode node = inners.next();

            if (node.isAnonymous()) // method compilation will take care
                continue;

            TypePolicy innerClassPolicy = getPolicy(node, source, classPolicy);

            allMethods(source, toProcess, node, innerClassPolicy);
        }
    }

    public static TypePolicy getPolicy(AnnotatedNode ann, SourceUnit source, TypePolicy def) {
        if(ann == null)
            return def;

        List<AnnotationNode> list;
        list = ann.getAnnotations(DYNAMIC_TYPE);
        if(!list.isEmpty()) {
        	return TypePolicy.DYNAMIC; 
        }
        list = ann.getAnnotations(MIXED_TYPE);
        if(!list.isEmpty()) {
        	return TypePolicy.MIXED;
        }
        list = ann.getAnnotations(COMPILE_TYPE);
        if (list.isEmpty())
            return def;
        
        if(checkDuplicateTypedAnn(list, source)) return null;
        
        for (AnnotationNode an : list) {
            final Expression member = an.getMember("value");
            if (member instanceof PropertyExpression) {
                PropertyExpression pe = (PropertyExpression) member;
                if (pe.getObjectExpression() instanceof ClassExpression) {
                    ClassExpression ce = (ClassExpression) pe.getObjectExpression();

                    if (ce.getType().getName().equals("groovy.lang.TypePolicy")) {
                        if ("DYNAMIC".equals(pe.getPropertyAsString())) {
                            return TypePolicy.DYNAMIC;
                        } else {
                            if ("MIXED".equals(pe.getPropertyAsString())) {
                                return TypePolicy.MIXED;
                            } else {
                                if ("STATIC".equals(pe.getPropertyAsString())) {
                                    return TypePolicy.STATIC;
                                }
                            }
                        }
                    }
                }
            }

            if (member == null) {
                continue;
            }

            int line = ann.getLineNumber();
            int col = ann.getColumnNumber();
            source.getErrorCollector().addError(
                    new SyntaxErrorMessage(new SyntaxException("Wrong 'value' for @Typed annotation" + '\n', line, col), source), true
            );
            return null;
        }
        return TypePolicy.STATIC;
    }
    
    private static boolean checkDuplicateTypedAnn(List<AnnotationNode> list, SourceUnit source) {
    	if(list.size() > 1) {
    		AnnotationNode secondAnn = list.get(1);
            int line = secondAnn.getLineNumber();
            int col = secondAnn.getColumnNumber();
            source.getErrorCollector().addError(
                    new SyntaxErrorMessage(new SyntaxException("Duplicate @Typed annotation found" + '\n', line, col), source), true
            );
            return true;
    	}
    	return false;
    }
}
