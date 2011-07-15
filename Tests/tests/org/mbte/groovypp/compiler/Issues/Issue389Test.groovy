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

@Typed
package org.mbte.groovypp.compiler.Issues

public class Issue389Test extends GroovyShellTestCase {
    void testMe () {
        shell.evaluate """
@Typed package p

def counters = ['lala':0]
counters['lala']++
        """
    }

    void testMe2 () {
        shell.evaluate """
@Typed package p

Map<String,Integer> counters = [:].withDefault{0}

"a,a,b,a,b,b,a,a,b,b,b,a,b,a".split("\\\\,").each {
   counters[it]++
   //counters[it] += 1 => works
}
println counters
        """
    }
}
