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

import static groovy.util.test.CompileTestSupport.shouldCompile
import static groovy.util.test.CompileTestSupport.shouldNotCompile

@Typed
public class Issue131Test extends GroovyShellTestCase {
    void testAssignmentInClosureWithReferencedVarTypeMismatch () {
        try {
            shell.evaluate """
                @Typed class Test {
                    def foo(Reference<Number> x) {
                        def c = {it -> x = it}
                    }
                }
            """
            fail("Compilation should have failed as there is a type mismatch in 'Reference<Number> = Object' assignment")
        } catch(ex) {
            assert ex instanceof org.codehaus.groovy.control.MultipleCompilationErrorsException
            assert ex.message.contains('Invalid assignment: groovy.lang.Reference') && 
                ex.message.contains('is not assignable from java.lang.Object')
        }
    }
    
    void testAssignmentInClosureWithReferencedVarTypeMatching () {
        shouldCompile """
            @Typed class Test {
                def foo(Reference<Number> x) {
                    def c = {Number it -> x = it}
                }
            }
        """
    }

    void testAssignmentInClosureWithFinalVarTypeMatching () {
        try {
        shell.evaluate """
            @Typed class Test {
                def foo(Number x) {
                    def c = {Number it -> x = it}
                }
            }
        """
        }
        catch(ex) {
          assert ex instanceof org.codehaus.groovy.control.MultipleCompilationErrorsException
          assert ex.message.contains('Cannot modify final field Test$foo$1.x')
        }
    }
}