/*
 * Copyright 2009-2011 MBTE Sweden AB.
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
package org.mbte.groovypp.compiler

class MultiPropSetTest extends GroovyShellTestCase {
    void testMe () {
        shell.evaluate """
@Typed package p

class ClosureWrapper implements Cloneable {
   Closure action

   def clone () {
       ClosureWrapper cloned = super.clone()
       cloned [action: action.clone()[delegate: cloned] ]
   }

   def cloneClassic () {
       ClosureWrapper cloned = super.clone()
       Closure clonedClosure = action.clone()
       clonedClosure.delegate = cloned
       cloned.action = clonedClosure
   }
}

ClosureWrapper cw = [action: {} ]
ClosureWrapper cloned = cw.clone()
assert cloned.action.delegate === cloned
ClosureWrapper cloned2 = cw.clone()
assert cloned2.action.delegate === cloned2
        """
    }

    void testMe2 () {
        shell.evaluate """
@Typed package p

class A {
}

class B extends A {
    def prop
}

A u () {
    new B () [prop: 'lala']
}

assert ((B)u()).prop == 'lala'
        """
    }

    void testMe3 () {
        shell.evaluate """
@Typed package p

class B {
    def prop

    B (p) {
        prop = p
    }
}

B u () {
    B b
    b = ['mama'] [prop: 'lala']
}

assert u().prop == 'lala'
        """
    }

    void testMe4 () {
        shell.evaluate """
@Typed package p

class B {
    B prop
    def prop2

    B (p) {
        prop2 = p
    }
}

B u () {
    B b
    b = new B('mama')[prop: ['empty']].prop [prop2: 'lala']
}

assert u().prop2 == 'lala'
        """
    }
}
