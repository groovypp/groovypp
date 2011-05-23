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

import antlr.collections.AST;
import org.codehaus.groovy.antlr.AntlrParserPlugin;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.ParserPlugin;
import org.codehaus.groovy.control.ParserPluginFactory;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.syntax.ASTHelper;
import org.codehaus.groovy.syntax.SyntaxException;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;
import org.codehaus.groovy.classgen.Verifier;
import org.mbte.groovypp.compiler.flow.MapWithListExpression;
import org.objectweb.asm.Opcodes;

import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;


@GroovyASTTransformation(phase = CompilePhase.CONVERSION)
public class TraitASTTransform implements ASTTransformation, Opcodes {

    public TraitASTTransform() {
        CleaningVerifier.getCompilationUnit().getConfiguration().setPluginFactory(new GppParserPluginFactory());
    }

    public void visit(ASTNode[] nodes, final SourceUnit source) {
        ModuleNode module = (ModuleNode) nodes[0];
        List<ClassNode> toProcess = new LinkedList<ClassNode>();
        final boolean forceTyped = source.getName().endsWith(".gpp");

        processPackageLevelUseAnnotation(module);

        AnnotationNode pkgTypedAnn = getAnnotation(module.getPackage(), "Typed");
        for (ClassNode classNode : module.getClasses()) {
            VolatileFieldUpdaterTransform.addUpdaterForVolatileFields(classNode);

            boolean process = false;
            boolean typed = false;
            for (AnnotationNode ann : classNode.getAnnotations()) {
                final String withoutPackage = ann.getClassNode().getNameWithoutPackage();
                if (withoutPackage.equals("Trait")) {
                    process = true;
                }
                if (withoutPackage.equals("Typed")) {
                    typed = true;
                    ann.getClassNode().setRedirect(TypeUtil.TYPED);
                }
            }

            if (forceTyped && !typed) {
                typed = true;
                classNode.addAnnotation(pkgTypedAnn != null ? pkgTypedAnn : new AnnotationNode(TypeUtil.TYPED));
            }

            if (process) {
                toProcess.add(classNode);
                if (!typed) {
                    classNode.addAnnotation(pkgTypedAnn != null ? pkgTypedAnn : new AnnotationNode(TypeUtil.TYPED));
                }
            }
        }

        for (ClassNode classNode : toProcess) {
            if (classNode.isInterface() || classNode.isEnum() || classNode.isAnnotationDefinition()) {
                source.addError(new SyntaxException("@Trait can be applied only to <class>", classNode.getLineNumber(), classNode.getColumnNumber()));
                continue;
            }

            if (classNode.getDeclaredConstructors().size() > 0) {
                ConstructorNode constructor = classNode.getDeclaredConstructors().get(0);
                source.addError(new SyntaxException("Constructors are not allowed in traits", constructor.getLineNumber(), constructor.getColumnNumber()));
                continue;
            }

            if (classNode.getObjectInitializerStatements().size() > 0) {
                Statement initializer = classNode.getObjectInitializerStatements().get(0);
                source.addError(new SyntaxException("Object initializers are not allowed in traits", initializer.getLineNumber(), initializer.getColumnNumber()));
                continue;
            }

            int mod = classNode.getModifiers();
            mod &= ~Opcodes.ACC_FINAL;
            mod |= Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE;
            classNode.setModifiers(mod);

            String name = classNode.getNameWithoutPackage() + "$TraitImpl";
            String fullName = ASTHelper.dot(classNode.getPackageName(), name);

            InnerClassNode innerClassNode = new InnerClassNode(classNode, fullName, ACC_PUBLIC|ACC_STATIC|ACC_ABSTRACT, ClassHelper.OBJECT_TYPE, new ClassNode[]{classNode}, null);
            AnnotationNode typedAnn = new AnnotationNode(TypeUtil.TYPED);
            final Expression member = getAnnotation(classNode, "Typed").getMember("debug");
            if (member != null && member instanceof ConstantExpression && ((ConstantExpression)member).getValue().equals(Boolean.TRUE))
                typedAnn.addMember("debug", ConstantExpression.TRUE);
            innerClassNode.addAnnotation(typedAnn);

            innerClassNode.setGenericsTypes(classNode.getGenericsTypes());

            ClassNode superClass = classNode.getSuperClass();
            if (!ClassHelper.OBJECT_TYPE.equals(superClass)) {
                ClassNode[] ifaces = classNode.getInterfaces();
                ClassNode[] newIfaces = new ClassNode[ifaces.length + 1];
                newIfaces[0] = superClass;
                System.arraycopy(ifaces, 0, newIfaces, 1, ifaces.length);
                classNode.setSuperClass(ClassHelper.OBJECT_TYPE);
                classNode.setInterfaces(newIfaces);
            }

            classNode.getModule().addClass(innerClassNode);

            InnerClassNode categoryClassNode = null;

            for (FieldNode fieldNode : classNode.getFields()) {
                if (fieldNode.isStatic()) {
                    innerClassNode.addField(fieldNode);
                    continue;
                }

                final String getterName = "get" + Verifier.capitalize(fieldNode.getName());
                MethodNode getter = classNode.getGetterMethod(getterName);
                if (getter == null) {
                    getter = classNode.addMethod(getterName, ACC_PUBLIC | ACC_ABSTRACT, fieldNode.getType(), Parameter.EMPTY_ARRAY, ClassNode.EMPTY_ARRAY, null);
                    addFieldAnnotation(innerClassNode, fieldNode, getter);
                }

                // We need the second getter (and setter) to compile non-synthetic first one referring to the field.
                // The references inside the first one will be retargeted to the second one.
                final MethodNode realGetter = classNode.addMethod("get$" + fieldNode.getName(), ACC_PUBLIC |
                        ACC_ABSTRACT, fieldNode.getType(), Parameter.EMPTY_ARRAY, ClassNode.EMPTY_ARRAY, null);
                addFieldAnnotation(innerClassNode, fieldNode, realGetter);

                final String setterName = "set" + Verifier.capitalize(fieldNode.getName());
                Parameter valueParam = new Parameter(fieldNode.getType(), "$value");
                MethodNode setter = classNode.getSetterMethod(setterName);
                if (setter == null) {
                    setter = classNode.addMethod(setterName, ACC_PUBLIC | ACC_ABSTRACT, ClassHelper.VOID_TYPE, new Parameter[]{valueParam}, ClassNode.EMPTY_ARRAY, null);
                    addFieldAnnotation(innerClassNode, fieldNode, setter);
                }

                final MethodNode realSetter = classNode.addMethod("set$" + fieldNode.getName(), ACC_PUBLIC |
                        ACC_ABSTRACT, ClassHelper.VOID_TYPE, new Parameter[]{valueParam}, ClassNode.EMPTY_ARRAY, null);
                addFieldAnnotation(innerClassNode, fieldNode, realSetter);

                if (fieldNode.hasInitialExpression()) {
                    final Expression initial = fieldNode.getInitialValueExpression();
                    fieldNode.setInitialValueExpression(null);

                    final MethodNode initMethod = innerClassNode.addMethod("__init_" + fieldNode.getName(), ACC_PUBLIC | ACC_STATIC, ClassHelper.VOID_TYPE, new Parameter[]{new Parameter(classNode, "$self")}, ClassNode.EMPTY_ARRAY, new BlockStatement());

                    final PropertyExpression prop = new PropertyExpression(new VariableExpression("$self"), fieldNode.getName());
                    prop.setSourcePosition(fieldNode);
                    final CastExpression cast = new CastExpression(fieldNode.getType(), initial);
                    cast.setSourcePosition(initial);
                    final BinaryExpression assign = new BinaryExpression(prop, Token.newSymbol(Types.ASSIGN, -1, -1), cast);
                    assign.setSourcePosition(initial);
                    final ExpressionStatement assignExpr = new ExpressionStatement(assign);
                    assignExpr.setSourcePosition(initial);
                    ((BlockStatement)initMethod.getCode()).addStatement(assignExpr);
                }

                innerClassNode.addField(fieldNode);
            }

            classNode.getFields().clear();
            classNode.getProperties().clear();

            ArrayList<MethodNode> toRemove = null;
            for (MethodNode methodNode : classNode.getMethods()) {
                if (methodNode.getCode() == null)
                    continue;

                if (methodNode.isStatic()) {
                    if(categoryClassNode == null) {
                        name = classNode.getNameWithoutPackage() + "$TraitCategory";
                        fullName = ASTHelper.dot(classNode.getPackageName(), name);

                        categoryClassNode = new InnerClassNode(classNode, fullName, ACC_PUBLIC|ACC_STATIC|ACC_ABSTRACT, ClassHelper.OBJECT_TYPE, ClassNode.EMPTY_ARRAY, null);
                        categoryClassNode.addAnnotation(typedAnn);

                        classNode.getModule().addClass(categoryClassNode);

                        toRemove = new ArrayList<MethodNode>();

                        final List<AnnotationNode> annotations = classNode.getAnnotations(TypeUtil.USE);
                        AnnotationNode useAnnotation;
                        if(annotations == null || annotations.isEmpty()) {
                            useAnnotation = new AnnotationNode(TypeUtil.USE);
                            final ListExpression value = new ListExpression();
                            classNode.addAnnotation(useAnnotation);
                            useAnnotation.addMember("value", value);
                        }
                        else {
                            useAnnotation = annotations.get(0);
                        }

                        ListExpression expr = (ListExpression) useAnnotation.getMember("value");
                        expr.addExpression(new ClassExpression(categoryClassNode));
                    }

                    categoryClassNode.addMethod(methodNode);
                    toRemove.add(methodNode);
                    continue;
                }

                if (!methodNode.isPublic()) {
                    source.addError(new SyntaxException("Non-public methods are not allowed in traits", methodNode.getLineNumber(), methodNode.getColumnNumber()));
                }

                mod = methodNode.getModifiers();
                mod &= ~(Opcodes.ACC_FINAL | Opcodes.ACC_PROTECTED | Opcodes.ACC_PRIVATE);
                mod |= Opcodes.ACC_ABSTRACT | Opcodes.ACC_PUBLIC;
                methodNode.setModifiers(mod);

                Parameter[] parameters = methodNode.getParameters();

                Parameter[] newParameters = new Parameter[parameters.length + 1];
                final Parameter self = new Parameter(classNode, "$self");
                newParameters[0] = self;
                System.arraycopy(parameters, 0, newParameters, 1, parameters.length);

                MethodNode newMethod = innerClassNode.addMethod(methodNode.getName(), ACC_PUBLIC, methodNode.getReturnType(), newParameters, ClassNode.EMPTY_ARRAY, methodNode.getCode());
                ArrayList<GenericsType> gt = new ArrayList<GenericsType>();
                if (classNode.getGenericsTypes() != null)
                    for (int i = 0; i < classNode.getGenericsTypes().length; i++) {
                        GenericsType genericsType = classNode.getGenericsTypes()[i];
                        gt.add(genericsType);
                    }
                if (methodNode.getGenericsTypes() != null)
                    for (int i = 0; i < methodNode.getGenericsTypes().length; i++) {
                        GenericsType genericsType = methodNode.getGenericsTypes()[i];
                        gt.add(genericsType);
                    }

                if (!gt.isEmpty())
                    newMethod.setGenericsTypes(gt.toArray(new GenericsType[gt.size()]));

                AnnotationNode annotationNode = new AnnotationNode(TypeUtil.HAS_DEFAULT_IMPLEMENTATION);
                annotationNode.addMember("value", new ClassExpression(innerClassNode));
                methodNode.addAnnotation(annotationNode);

                methodNode.setCode(null);
            }

            if(toRemove != null) {
                for(MethodNode methodNode : toRemove) {
                    classNode.getMethods().remove(methodNode);
                }
            }
        }
    }

    private void processPackageLevelUseAnnotation(ModuleNode module) {
        AnnotationNode pkgUseAnn = getAnnotation(module.getPackage(), "Use");
        if(pkgUseAnn != null) {
            final Expression member = pkgUseAnn.getMember("value");
            if(member != null) {
                for (ClassNode classNode : module.getClasses()) {
                    final AnnotationNode use = getAnnotation(classNode, "Use");
                    if(use != null) {
                        final Expression value = use.getMember("value");
                        final ListExpression listExpression = new ListExpression();
                        if(value instanceof ListExpression) {
                            listExpression.getExpressions().addAll(((ListExpression)value).getExpressions());
                        }
                        else {
                            listExpression.getExpressions().add(value);
                        }
                        if(member instanceof ListExpression) {
                            listExpression.getExpressions().addAll(((ListExpression)member).getExpressions());
                        }
                        else {
                            listExpression.getExpressions().add(member);
                        }
                        use.setMember("value", listExpression);
                    }
                    else {
                        final AnnotationNode annotationNode = new AnnotationNode(TypeUtil.USE);
                        annotationNode.addMember("value", member);
                        classNode.addAnnotation(annotationNode);
                    }
                }
            }
        }
    }

    private AnnotationNode getAnnotation(AnnotatedNode node, String annName) {
    	AnnotationNode pkgTyped = null;
    	if(node != null) {
        	for (AnnotationNode ann : node.getAnnotations()) {
                final String withoutPackage = ann.getClassNode().getNameWithoutPackage();
                if (withoutPackage.equals(annName)) {
                	pkgTyped = ann;
                }
        	}
    	}
    	return pkgTyped;
    }
    
    private void addFieldAnnotation(InnerClassNode innerClassNode, FieldNode fieldNode, MethodNode getter) {
        AnnotationNode value = new AnnotationNode(TypeUtil.HAS_DEFAULT_IMPLEMENTATION);
        value.addMember("value", new ClassExpression(innerClassNode));
        value.addMember("fieldName", new ConstantExpression(fieldNode.getName()));
        getter.addAnnotation(value);
    }

    private static class GppParserPluginFactory extends ParserPluginFactory {
        public ParserPlugin createParserPlugin() {
            return new AntlrParserPlugin() {
                protected Expression declarationExpression(AST variableDef) {
                    final DeclarationExpression expression = (DeclarationExpression) super.declarationExpression(variableDef);
                    final VariableExpression variableExpression = expression.getVariableExpression();
                    if(!ClassHelper.isPrimitiveType(variableExpression.getOriginType())) {
                        variableExpression.setType(variableExpression.getOriginType());
                    }
                    return expression;
                }

                protected Expression mapExpression(AST mapNode) {
                    List expressions = new ArrayList();
                    ListExpression list = null;
                    AST elist = mapNode.getFirstChild();
                    if (elist != null) {  // totally empty in the case of [:]
                        assertNodeType(ELIST, elist);
                        for (AST node = elist.getFirstChild(); node != null; node = node.getNextSibling()) {
                            switch (node.getType()) {
                                case LABELED_ARG:
                                case SPREAD_MAP_ARG:
                                    expressions.add(mapEntryExpression(node));
                                    break;  // legal cases
                                case SPREAD_ARG:
                                    assertNodeType(SPREAD_MAP_ARG, node);
                                    break;  // helpful error
                                default:
                                    if(list == null)
                                        list = new ListExpression();
                                    list.addExpression(expression(node));
                                    break;
                            }
                        }
                    }
                    if(list == null) {
                        MapExpression mapExpression = new MapExpression(expressions);
                        configureAST(mapExpression, mapNode);
                        return mapExpression;
                    }
                    else {
                        MapWithListExpression mapExpression = new MapWithListExpression(expressions, list);
                        configureAST(mapExpression, mapNode);
                        return mapExpression;
                    }
                }
            };
        }
    }
}
