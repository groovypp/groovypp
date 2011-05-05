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
package org.mbte.groovypp.compiler

import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit

import org.codehaus.groovy.syntax.Token
import org.codehaus.groovy.syntax.Types
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

import org.objectweb.asm.Opcodes
import org.codehaus.groovy.ast.*
import static org.codehaus.groovy.ast.ClassHelper.make
import org.codehaus.groovy.ast.expr.*
import org.codehaus.groovy.syntax.ASTHelper

import org.codehaus.groovy.classgen.Verifier

@Typed
@GroovyASTTransformation (phase = CompilePhase.CANONICALIZATION)
class StructASTTransform implements ASTTransformation, Opcodes {

    static final ClassNode ASTRUCT = make(FObject)
    static final ClassNode ABUILDER = make(FObject.Builder)
    static final ClassNode STRING_BUILDER = make(StringBuilder)

    void visit(ASTNode[] nodes, SourceUnit source) {
        ModuleNode module = nodes[0]
        for (ClassNode classNode: new ArrayList(module.classes)) {
            processClass classNode, source
        }
    }

    private void processClass (ClassNode classNode, SourceUnit source) {
        def typed = false, process = false
        for (AnnotationNode ann : classNode.getAnnotations()) {
            final String withoutPackage = ann.getClassNode().getNameWithoutPackage();
            if (withoutPackage.equals("Struct")) {
                process = true;
            }
            if (withoutPackage.equals("Typed")) {
                typed = true;
            }
        }

        if (process) {
            String name = classNode.getNameWithoutPackage() + "\$Builder";
            String fullName = ASTHelper.dot(classNode.getPackageName(), name);

            for(c in classNode.module.classes)
                if (c.name == fullName)
                    return

            if (!typed)
                classNode.addAnnotation(new AnnotationNode(TypeUtil.TYPED))

            ClassNode superClass = classNode.getUnresolvedSuperClass(false);
            if (ClassHelper.OBJECT_TYPE.equals(superClass)) {
                superClass = TypeUtil.withGenericTypes(ASTRUCT, (GenericsType[])null)
                classNode.setSuperClass(superClass)
            }

            ClassNode superBuilder
            for(ClassNode bc : superClass.redirect().getInnerClasses()) {
                if (bc.getName().endsWith("\$Builder")) {
                    superBuilder = bc
                    break;
                }
            }

            if (!superBuilder) {
                if(superClass.module) {
                    processClass(superClass, superClass.module.getContext())
                }
                else {
                    superBuilder = ABUILDER
                }
            }

            InnerClassNode builderClassNode = new InnerClassNode(classNode, fullName, ACC_PUBLIC|ACC_STATIC, superBuilder, ClassNode.EMPTY_ARRAY, null)
            AnnotationNode typedAnn = new AnnotationNode(TypeUtil.TYPED)
            final Expression member = classNode.getAnnotations(TypeUtil.TYPED).get(0).getMember("debug")
            if (member != null && member instanceof ConstantExpression && ((ConstantExpression)member).value.equals(Boolean.TRUE))
                typedAnn.addMember("debug", ConstantExpression.TRUE)
            builderClassNode.addAnnotation(typedAnn)

            builderClassNode.genericsTypes = classNode.genericsTypes
            builderClassNode.superClass = TypeUtil.withGenericTypes(superBuilder.redirect(), superClass.genericsTypes);

            classNode.getModule().addClass(builderClassNode);

            SerialASTTransform.processClass(classNode, source)

            addFactoryMethods (classNode, builderClassNode)

            classNode.properties.clear ()

            for (f in classNode.fields) {
                if (!f.static && !(f.name == "metaClass")) {
                    f.modifiers = (f.modifiers & ~(Opcodes.ACC_PUBLIC & Opcodes.ACC_PROTECTED)) | Opcodes.ACC_PRIVATE
                    addFieldMethods(f, classNode, builderClassNode)
                }
            }

            addToString (classNode)
        }

        for( c in classNode.innerClasses)
            processClass(c, source)
    }

    def addFactoryMethods(ClassNode classNode, ClassNode builderClass) {
//    static Builder asMutable(StructTest<A> self = null) {
//        new Builder(self)
//    }
        classNode.addMethod(
                "asMutable",
                Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,
                builderClass,
                [[classNode, "obj"]],
                [], new ExpressionStatement(
                        new ConstructorCallExpression(
                                builderClass,
                                new CastExpression(
                                        classNode,
                                        new MethodCallExpression(new VariableExpression("obj"), "clone", new ArgumentListExpression())
                                )
                        )
                )
        )

        builderClass.addMethod(
                "asImmutable",
                Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,
                classNode,
                [[builderClass, "self"]],
                [], new ExpressionStatement(
                        new CastExpression(
                                classNode,
                                new MethodCallExpression(new VariableExpression("self"), "getAndForget", new ArgumentListExpression())
                        )
                )
        )

//    static class Builder<A> extends FObject.Builder {
//        Builder (T obj) {
//            super(obj)
//        }
        builderClass.addConstructor(
            Opcodes.ACC_PROTECTED,
            [[classNode, "obj"]],
            [],
            new ExpressionStatement(
                new ConstructorCallExpression(
                    ClassNode.SUPER,
                    new ArgumentListExpression(
                        new VariableExpression("obj")
                    )
                )
            )
        )
    }

    void addFieldMethods(FieldNode fieldNode, ClassNode classNode, InnerClassNode innerClassNode) {
        fieldNode.modifiers &= ~(Opcodes.ACC_PUBLIC|Opcodes.ACC_PROTECTED)
        fieldNode.modifiers |= Opcodes.ACC_PRIVATE

        classNode.addMethod(
            "get" + Verifier.capitalize(fieldNode.name),
            Opcodes.ACC_PUBLIC,
            fieldNode.type,
            [],
            [], new ExpressionStatement(
                new PropertyExpression(
                    VariableExpression.THIS_EXPRESSION,
                    fieldNode.name
                )
            )
        )

        innerClassNode.addMethod(
            "get" + Verifier.capitalize(fieldNode.name),
            Opcodes.ACC_PUBLIC,
            fieldNode.type,
            [],
            [], new ExpressionStatement(
                new PropertyExpression(
                    new CastExpression(
                        classNode,
                        new PropertyExpression(VariableExpression.THIS_EXPRESSION,"obj")
                    ),
                    fieldNode.name
                )
            )
        )

        if (!fieldNode.final) {
            innerClassNode.addMethod(
                "set" + Verifier.capitalize(fieldNode.name),
                Opcodes.ACC_PUBLIC,
                ClassHelper.VOID_TYPE,
                [[fieldNode.type, "value"]],
                [], new ExpressionStatement(
                    new BinaryExpression(
                        new PropertyExpression(
                            new CastExpression(
                                    classNode,
                                    new PropertyExpression(VariableExpression.THIS_EXPRESSION,"obj"),
                            ),
                            fieldNode.name
                        ),
                        Token.newSymbol(Types.ASSIGN,-1,-1),
                        new VariableExpression("value")
                    )
                )
            )
        }
    }

    void addToString(ClassNode classNode) {
        def code = new BlockStatement ()

        code.addStatement(
            new ExpressionStatement(
                new MethodCallExpression(
                    VariableExpression.SUPER_EXPRESSION,
                    "toString",
                    new ArgumentListExpression(new VariableExpression("sb"))
                )
            )
        )

        boolean nonFirst = false
        for(def cn = classNode.superClass; !nonFirst && cn; cn = cn.superClass) {
            for(ff in cn.fields) {
                if (!ff.static && !(ff.name == "metaClass")) {
                    nonFirst = true
                    break
                }
            }
        }

        for (f in classNode.fields) {
            if (!f.static && !(f.name == "metaClass")) {
                if(nonFirst) {
                    code.addStatement(
                        new ExpressionStatement(
                            new MethodCallExpression(
                                new VariableExpression("sb"),
                                "append",
                                new ArgumentListExpression(new ConstantExpression(", "))
                            )
                        )
                    )
                }
                nonFirst = true

                code.addStatement(
                    new ExpressionStatement(
                        new MethodCallExpression(
                            new MethodCallExpression(
                                new VariableExpression("sb"),
                                "append",
                                new ArgumentListExpression(new ConstantExpression(f.name + ": "))
                            ),
                            "append",
                            new ArgumentListExpression(new FieldExpression(f))
                        )
                    )
                )
            }
        }


        classNode.addMethod(
                "toString",
                Opcodes.ACC_PUBLIC,
                ClassHelper.VOID_TYPE,
                [[STRING_BUILDER, "sb"]],
                [],
                code
        )
    }
}

//class StructTest<A> extends FObject {
//    private int x, y
//    private A inner
//
//    StructTest () {
//    }
//
//    StructTest (int x, int y) {
//        this.x = x
//        this.x = y
//    }
//
//    static Builder newBuilder(StructTest<A> self = null) {
//        new Builder(self)
//    }
//
//    int getX ()   { x }
//    int getY ()   { y }
//    A getInner () { inner }
//
//    String toString() { "[x:$x, y:$y, inner:$inner]" }
//
//    int hashCode () {
//        int h = 1
//        h = 31*h + x
//        h = 31*h + y
//        h = 31*h + inner?.hashCode ()
//        h
//    }
//
//    boolean equals (Object o) {
//        if (o instanceof StructTest) {
//            StructTest other = o
//            other.x == x && other.y == y && other.inner == inner
//        }
//    }
//
//    static class Builder<T extends StructTest,A> extends FObject.Builder<T> {
//        Builder (T obj = null) {
//            super(obj != null ? (T)obj.clone() : new StructTest())
//        }
//
//        int  getX ()      { obj.x     }
//        void setX (int x) { obj.x = x }
//
//        int  getY ()      { obj.y     }
//        void setY (int x) { obj.y = y }
//
//        A    getInner ()        { obj.inner }
//        void setInner (A inner) { obj.inner = inner }
//    }
//
//    static class StringTupleTest extends StructTest<String> {
//        static class Builder<T extends StringTupleTest> extends StructTest.Builder<T,String> {
//            Builder (T obj = null) {
//                super(obj != null ? (T)obj.clone() : new StringTupleTest())
//            }
//        }
//
//        static Builder newInstance() {
//            new Builder()
//        }
//
//        Builder builder () {
//            new Builder(this)
//        }
//    }
//
//    static void main (String [] args ) {
//        def b = StringTupleTest.newInstance()
//        b.x = 1
//        b.y = 2
//        b.inner = "mama"
//        println "$b ${b.inner.toUpperCase()}"
//
//        def o = b.build()
//        println o
//    }
//}
