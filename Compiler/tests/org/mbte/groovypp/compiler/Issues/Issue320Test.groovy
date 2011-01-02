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

class Issue320Test extends GroovyShellTestCase {
    void testStaticClosureWithMetaClassAsEMC() {
        try {
            shell.evaluate """
                package org.example
                
                @Typed(TypePolicy.MIXED)
                class TestController {
                    static accessControl = {
                        role(name: 'Administrator', action: 'rollbackWikiVersion' )
                    }
                }
                
                def staticClosure = TestController.accessControl
                
                // Grails's default meta-class is EMC. So, replace MC for the static closure to reproduce the issue 
                staticClosure.metaClass = new ExpandoMetaClass(staticClosure.class) 
                staticClosure.metaClass.initialize()
                
                staticClosure()
            """
        } catch (ex) {
            if(ex instanceof MissingMethodException) {
                /*
                 recieving MissingMethodException with the following message is that static closure's 
                 owner/thisObject got set appropriately and its methodMissing tried to its job, but, as
                 expected, failed to find role([:]) method here 
                */
                assert ex.message.contains('No signature of method: static org.example.TestController.role()')
            } else {
                throw ex
            }
        }
    }
}
