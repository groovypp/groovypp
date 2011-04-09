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

public class ConstructorCallTest extends GroovyShellTestCase {

    void testNoParam () {

    }

    void testNoParamButMap () {

    }

    void testThis () {

    }

    void testSuper () {

    }

    void testAnonimous () {
        shell.evaluate """
@Typed package p

abstract class A {
    int v

    A (int v) {
        this.v = v
    }

    abstract int act ()
}

def x = 10
def obj = new A (12){
    int act() {
        x + v
    }
}
def res = obj.act ()
assert res == 22
        """
    }
}