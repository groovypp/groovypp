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
package org.mbte.groovypp.compiler.Issues

class Issue309Test extends GroovyShellTestCase {
  void testMe () {
    shell.evaluate """
@Typed package test

@Field res = []

def foo() {
    @Field mf = 5
    mf++
    res << mf
}

println "@Field usage inside closure"
(1..2).each{
    @Field cf = 5
    cf++
    res << cf
}

println "@Field usage inside method"
(1..2).each{
    foo()
}

assert res == [6, 7, 6, 7]
        """
  }
}
