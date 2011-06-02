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

class Issue340Test extends GroovyShellTestCase {
  void testMe () {
    shell.evaluate """
        @Typed package p

        class Issue340TestHelper {
            String ISSUE_ID = "Issue340"
            
            static main(args) {
                def i340 = new Issue340TestHelper()
                i340.testMe()
            }
            
            void testMe() {
                assert getLengthInMixedMode(ISSUE_ID) == 8
                assert getLengthInDynamicMode(ISSUE_ID) == 8
            }
        
            /* 
                In STATIC mode (defined at pkg level), the following 2 methods won't compile as type of param 'str'
                is not known and static compilation finds the method call length() invalid on Object. We are testing 
                if use of @Mixed and @Dynamic correctly overrides that correctly and let's this code compile 
            */
            
            @Mixed
            private getLengthInMixedMode(str) {
                str.length()
            }
            
            @Dynamic
            private getLengthInDynamicMode(str) {
                str.length()
            }
        }
    """
  }
}
