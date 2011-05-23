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

    void testTypeinference0(){
        shell.evaluate  """

          @Typed(debug=true) u0(obj) {
                obj instanceof String ? obj.toUpperCase() : ((Object[])obj)
          }

          assert u0("abc") == "ABC"
          """
    }

    void testTypeinference1(){
        shell.evaluate  """

          @Typed boolean u1(obj) {
                obj instanceof Pair && obj.first
          }

          assert u1((Pair)[10, 12])
      """
    }

    void testTypeinference2(){
        shell.evaluate  """
          @Typed def u2(obj) {
                obj instanceof Pair ? obj.first : 'wrong'
          }

          assert u2((Pair)[10, 12]) == 10

      """
    }

    void testTypeinference3(){
        shell.evaluate  """
          @Typed def u3(obj) {
                if(obj instanceof Pair)
                    obj.first = 11
                obj
          }

          assert u3((Pair)[10, 12]).first == 11

      """
    }

    void testTypeinference4(){
        shell.evaluate  """
          @Typed def u4(obj) {
                if(!(obj instanceof Pair))
                    throw new RuntimeException()
                else
                    obj.second = 50
                obj
          }

          assert u4((Pair)[10, 12]).second == 50

      """
    }

    void testTypeinference5(){
        shell.evaluate  """
          @Typed boolean u5(obj) {
                !(obj instanceof Pair) || obj.first
          }

          assert u5((Pair)[10, 12])

      """
    }

    void testTypeinference6(){
        shell.evaluate  """
          @Typed def u6(obj) {
                def res = obj instanceof Pair ? obj.first : 'wrong'
                res
          }

          assert u6((Pair)[10, 12])

      """
    }

    void testTypeinference7(){
        shell.evaluate  """
          @Typed def u7(obj) {
                def res = !(obj instanceof Pair) ? 'wrong':  obj.first
                res
          }

          assert u7((Pair)[10, 12])

      """
    }

    void testTypeinference8(){
        shell.evaluate  """
        @Typed int u8(def p){
              if(p instanceof Pair && p.second) {
                  (Integer)p.first
              }
        }

        assert u8((Pair)[first:12, second:5]) == 12
        assert u8((Pair)[first:12, second:0]) == 0
        assert u8([]) == 0

      """
    }

    void testTypeinference9(){
        shell.evaluate  """
          @Typed(debug=true) int u9(def p){
                if(!(p instanceof Pair) || p.second) {
                    1
                }
          }

          assert u9((Pair)[first:12, second:5]) == 1
          assert u9((Pair)[first:12, second:0]) == 0
          assert u9([]) == 1

      """
    }

    void testTypeinference10(){
        shell.evaluate  """
            @Typed def u10 () {
                def l = []
                if(l) {
                    l = [:]
                }
                else {
                    l << 10
                }
            }
            assert u10() == [10]
      """
    }
}
