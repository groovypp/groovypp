/*
 * Copyright 2009-2010 MBTE Sweden AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package groovypp.languages

import org.mbte.groovypp.compiler.languages.LanguageDefinition
import org.mbte.groovypp.compiler.languages.ScriptLanguageDefinition

class LanguageTest extends GroovyShellTestCase {
    void testMe () {
        def cls = shell.classLoader.parseClass("""
package groovypp.languages

import org.mbte.groovypp.compiler.languages.ScriptLanguageDefinition as scriptLanguage

class AbstractObject {
}

baseClass: ClassHelper.make(AbstractObject)

def oldConversion = conversion
conversion: { moduleNode ->
    oldConversion.execute(moduleNode)
}

void handleStatement(ClassNode clazz, Statement statement, BlockStatement constructorCode) {
    switch(statement) {
        case ExpressionStatement:
            def expression = statement.expression
            if(expression instanceof DeclarationExpression) {
                DeclarationExpression decl = expression
                if(decl.leftExpression instanceof VariableExpression) {
                    VariableExpression ve = decl.leftExpression
                    clazz.addField(ve.name, Opcodes.ACC_PUBLIC, ve.type, decl.rightExpression)
                    statement.expression = EmptyExpression.INSTANCE
                    return
                }
            }
    }

    super.handleStatement(clazz, statement, constructorCode)
}

""")
        assertEquals cls.superclass, ScriptLanguageDefinition
        def instance = cls.newInstance()

        assertNotNull instance.conversion
        assertTrue instance.conversion instanceof LanguageDefinition.ModuleOp

        def cls2 = cls.classLoader.parseClass("""
package groovypp.languages

import ${cls.name} as scriptLanguage

Pair<Integer,Integer> dimension = [0,12]
"""
        )
        assertEquals cls2.superclass.name, 'groovypp.languages.AbstractObject'
        assertEquals cls2.newInstance().dimension.second, 12
    }
}
