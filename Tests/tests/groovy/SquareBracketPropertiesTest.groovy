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
package groovy

class SquareBracketPropertiesTest extends GroovyShellTestCase {
    void testSetProps () {
        shell.evaluate """
            @Typed package p

            def p = new Pair<Integer,Integer> ()[first:1, second:-1]
            assert p.first  ==  1
            assert p.second == -1

            p[first:15 + p.second, second:10 + p.first]
            assert p.first  == 14
            assert p.second == 24
        """
    }

    void testClass () {
        shell.evaluate """
            @Typed package p

            def p = Pair[first:10, second:-1]
            assert p.first  == 10
            assert p.second == -1
        """
    }

    void testList () {
        shell.evaluate """
            @Typed package p

            def p = Pair[5, 8]
            assert p.first  == 5
            assert p.second == 8
        """
    }

    void testMixedListMap () {
        shell.evaluate """
            @Typed package p

class XPair {
    def first, second
    XPair(f, s) { this[first:f, second:s] }

    String toString() {"\$first \$second"}
}
            def q = XPair[5, 8, first:10, second:-1]
            assert q.class == XPair
            assert q.toString () == '10 -1'

            XPair qq = [5, 8, first:10, second:-1, toString: { "first: \$first, second: \${((int)q.second)+239}" } ]
            assert qq.class != XPair
            assert qq.toString () == 'first: 10, second: 238'
        """
    }

    void testSample () {
        shell.evaluate """
            @Typed package p

class X {
   int a
   String b
   List c
   float d
}

X var = []

def u = var[ a : 10, b : 12][c:[0,1,2]][d: 239]
assert u === var
assert var.a == 10
assert var.b == '12' // yes, we converted 12 to '12' automatically
assert var.c == [0,1,2]
assert var.d == 239
        """
    }
}
