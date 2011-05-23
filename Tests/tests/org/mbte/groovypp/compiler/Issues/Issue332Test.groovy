/*
<<<<<<< HEAD
 * Copyright 2009-2010 MBTE Sweden AB.
=======
 * Copyright 2009-2011 MBTE Sweden AB.
>>>>>>> 9bfc35d585187c1f4f7d53629b184e7104822dea
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

class Issue332Test extends GroovyShellTestCase {
    void testPrimitiveBooleanForLoop () {
        shell.evaluate """
            @Typed package primitiveloops
            
            boolean[] ary = new boolean[2]
            for (boolean v in ary) {}
        """
    }

    void testPrimitiveByteForLoop () {
        shell.evaluate """
            @Typed package primitiveloops
            
            byte[] ary = new byte[2]
            for (byte v in ary) {}
        """
    }

    void testPrimitiveCharForLoop () {
        shell.evaluate """
            @Typed package primitiveloops
            
            char[] ary = new char[2]
            for (char v in ary) {}
        """
    }

    void testPrimitiveShortForLoop () {
        shell.evaluate """
            @Typed package primitiveloops
            
            short[] ary = new short[2]
            for (short v in ary) {}
        """
    }
}
