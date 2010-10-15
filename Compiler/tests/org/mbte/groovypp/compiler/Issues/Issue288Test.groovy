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

package org.mbte.groovypp.compiler.Issues

@Typed class Issue288Test extends GroovyShellTestCase
{
    void testMe()
    {
        def res = shell.evaluate("""
        @Typed package p
        def foo(Integer a = 1, List<String> args, File... varargs) {
           a
        }

//        foo(["a", "c"], ["b"])
        """)
//        assert res == 1
    }

    void testCharStringGString () {
        def head = 'abcd'
        assert "h".length() == 1
        assert "head".indexOf('e') == 1
        assert "head".indexOf((int)'a') == 2
        assert "[$head]".replace('c', File.separatorChar).toUpperCase() == "[AB${File.separatorChar}D]"
    }
}
