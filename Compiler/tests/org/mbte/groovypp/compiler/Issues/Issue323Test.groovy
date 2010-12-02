/*
 * Copyright 2009-2010 MBTE Sweden AB.
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

package org.mbte.groovypp.compiler.Issues

import static groovy.util.test.CompileTestSupport.shouldCompile

@Typed
class Issue323Test extends GroovyShellTestCase {

    void testMe()
    {
        shell.evaluate """
@Typed(value=TypePolicy.MIXED,debug=true) package p

class MyClass {
    public String name

    String getName() { return name }
    void setName(String name) { this.name = name.toUpperCase() }

    void woof() { name = "Fido" }
    MyClass changeName(String newName) {
        newName.each { this.@name += it.toLowerCase() }
        this
    }
}

assert new MyClass(name:'LaLaLa').changeName('ABCD').@name == 'LALALAabcd'
        """
    }
}