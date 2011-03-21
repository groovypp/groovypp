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

class InstanceofTest extends GroovyShellTestCase {

    void testTrue() {
      shell.evaluate  """

          @Typed
          def u() {
            def x = false
            def o = 12

            if ( o instanceof Integer ) {
                x = true
            }

            assert x == true
          }

          u()
        """
    }
    
    void testFalse() {
      shell.evaluate  """

          @Typed
          def u() {
            def x = false
            def o = 12

            if ( o instanceof Double ) {
                x = true
            }

            assert x == false

          }

          u()
        """
    }
    
    void testImportedClass() {
        shell.evaluate  """

          @Typed
          def u() {
            def m = ["xyz":2]
            assert m instanceof Map
            assert !(m instanceof Double)

          }

          u()
        """
    }
    
    void testFullyQualifiedClass() {
        shell.evaluate  """

          @Typed
          def u() {
            def l = [1, 2, 3]
            assert l instanceof java.util.List
            assert !(l instanceof Map)
            
          }

          u()
        """
    }
    
    void testBoolean(){
        shell.evaluate  """

          @Typed
          def u() {
             assert true instanceof Object
             assert true==true instanceof Object
             assert true==false instanceof Object
             assert true==false instanceof Boolean
             assert !new Object() instanceof Boolean
          }

          u()
        """
    }

    void testTypeinference(){
        shell.evaluate  """

          @Typed boolean u1(obj) {
                obj instanceof Pair && obj.first
          }

          @Typed def u2(obj) {
                obj instanceof Pair ? obj.first : 'wrong'
          }

          @Typed def u3(obj) {
                if(obj instanceof Pair)
                    obj.first = 11
                obj
          }

          @Typed def u4(obj) {
                if(!(obj instanceof Pair))
                    throw new RuntimeException()
                else
                    obj.second = 50
                obj
          }

          @Typed boolean u5(obj) {
                !(obj instanceof Pair) || obj.first
          }

          assert u1((Pair)[10, 12])
          assert u2((Pair)[10, 12]) == 10
          assert u3((Pair)[10, 12]).first == 11
          assert u4((Pair)[10, 12]).second == 50
          assert u5((Pair)[10, 12])
        """
    }
}
