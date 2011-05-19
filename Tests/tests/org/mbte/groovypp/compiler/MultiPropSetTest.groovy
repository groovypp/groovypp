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
}
