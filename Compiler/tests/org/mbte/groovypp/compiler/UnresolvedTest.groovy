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

class UnresolvedTest extends GroovyShellTestCase {
    void testMe () {
        shell.evaluate """
@Typed package p

class A {
    Map<String,String> map = [:]

    String getUnresolvedProperty (String name) {
        map [name]
    }

    void setUnresolvedProperty (String name, String value) {
        map [name] = value
    }
}
       def map = new A()
       map.test = '12'
       assert map.test == '12'
       def aa = (map.test = 'aa').toUpperCase()
       assert aa == 'AA'
        """
    }

    void testInt () {
        shell.evaluate """
@Typed package p

class A {
    Map<String,Object> map = [:]

    Object getUnresolvedProperty (String name) {
        map [name]
    }

    void setUnresolvedProperty (String name, Object value) {
        map [name] = value
    }
}
       def map = new A()

       Integer cnt = 10
       map.lambada = cnt + 229
       assert map.lambada == 239
        """
    }
}
