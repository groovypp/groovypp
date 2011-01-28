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

package groovy

class PowerAssertTest extends GroovyShellTestCase {
    @Typed void testMe () {
        def code = """
       try {
           def i = 9
           println i.class
           def list = []
           assert (list[0] = 'aaaa') && !((i += 2.toString()  + [3].class.simpleName) != 10)
       }
       catch(AssertionError e) {
           println e.message
           println()
           return e.message
       }
        """
        shell.evaluate """
@Typed def staticCall () {
$code
}

def dynamicCall () {
$code
}

assert staticCall () == dynamicCall ()
        """
    }
}
