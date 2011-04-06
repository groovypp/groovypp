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

class Issue358Test extends GroovyShellTestCase {
    void testMe () {
        try {
            shell.evaluate """
    @Typed
    class Dummy {
        private static final list = [1, 2]
        static main(args) {
            assert 1 in [1, 2] // works
            assert 1 in list // does not work
        }
    }
        """
        }
        catch(e) {
            assert e.message.contains("Operator 'in' does not applicable to java.lang.Object")
        }
    }
}
