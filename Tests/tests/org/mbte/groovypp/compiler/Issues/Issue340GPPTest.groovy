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

class Issue340GPPTest extends GroovyTestCase {

    private File dynSrcDir
    private File gppTestScript

    private List allFiles = [gppTestScript]

    public void setUp(){
        super.setUp()
        locateCompilerTestDir()
        
        gppTestScript = new File(dynSrcDir, 'Issue340GPPTestHelper.gpp')
        gppTestScript.delete()
        gppTestScript << """
            class Issue340GPPTestHelper {
                String ISSUE_ID = "Issue340"
                
                static main(args) {
                    def i340 = new Issue340GPPTestHelper()
                    i340.testMe()
                    throw new RuntimeException('Issue340GPPTest successful')
                }
                
                void testMe() {
                    assert getLengthInMixedMode(ISSUE_ID) == 8
                    assert getLengthInDynamicMode(ISSUE_ID) == 8
                }
            
                /* 
                    It's a *.gpp file, so in STATIC mode, the following 2 methods won't compile as type of param 'str'
                    is not known and static compilation finds the method call length() invalid on Object. We are testing 
                    if use of @Mixed and @Dynamic correctly overrides that in this gpp file and let's this code compile 
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
    
    public void tearDown(){
        try {
            super.tearDown()
            allFiles*.delete()
        } catch(Exception ex){
            throw new RuntimeException("Could not delete source files dynamically created in " + dynSrcDir, ex)
        }
    }

    void testGPPFilesBeingResolvedToScript() {
        GroovyShell shell = new GroovyShell(this.class.classLoader)
        try {
            shell.evaluate(gppTestScript)
            fail('If script Issue340GPPTestHelper was evaluated successfully, it should have thrown RuntimeException.')
        } catch(RuntimeException ex) {
            assert ex.message.contains('Issue340GPPTest successful')
        }  
    }
    
    private void locateCompilerTestDir(){
        String bogusFile = "bogusFile"
        File f = new File(bogusFile)
        String path = f.getAbsolutePath()
        path = path.substring(0, path.length() - bogusFile.length())
        dynSrcDir = new File(path + File.separatorChar + "build" + File.separatorChar + 'classes' + File.separatorChar + 'test')
    }

}