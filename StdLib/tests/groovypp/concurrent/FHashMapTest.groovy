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

package groovypp.concurrent

@Typed
public class FHashMapTest extends GroovyTestCase {
    void testInsert () {
        def map = FHashMap.emptyMap
        FHashMap m = map.put(10,-1).put(2,-2).put(2,-3)
        assertEquals 2, m.size()
        assertEquals (-3,m [2])
        assertEquals (-1,m [10])

        m = m.put(34, -11)
        assertEquals (-11,m [34])
        assertEquals (-3,m [2])
        assertEquals (-1,m [10])

        assertNull m.get(1222)
        assertEquals (17,m.get(1222,17))
        assertEquals (177,m.getOr(1222){177})

        assertEquals 3, m.size()

        m = m.put("mama", "papa")
        assertEquals "papa", m.mama
    }

    static class Collision {
      int value

      int hashCode () {
        value % 7
      }

      boolean equals(Object c) {
        (c instanceof Collision) && ((Collision)c).value == value
      }
    }


    void testInsertCollisions () {
      def map = FHashMap.emptyMap
      def k0 = new Collision(value: 0)
      def m = map.put(k0, 0)
      def k7 = new Collision(value: 7)
      m = m.put(k7, 7)
      def k14 = new Collision(value: 14)
      m = m.put(k14, 14)

      assert m[k0] == 0
      assert m[k7] == 7
      assert m[k14] == 14

      assert m instanceof FHashMap.CollisionNode

      m = m.remove(k0)
      assert m[k0] === null
      assert m[k7] == 7
      assert m[k14] == 14
      assert m instanceof FHashMap.CollisionNode

      def k239 = new Collision(value: 239)
      m = m.put(k239, 239)

      assert m instanceof FHashMap.BitmappedNode

      assert m[k7] == 7
      assert m[k14] == 14
      assert m[k239] == 239

      for(e in m) {
        println "$e.key $e.value"
      }
    }

    void testInsertMany () {
        Map<Integer,Integer> data = [:]
        def clock = System.currentTimeMillis()
        for(i in 0..<500000) {
            data[i] = -i
        }
        println("Map box & insert: ${System.currentTimeMillis()-clock}")

        def map = FHashMap.emptyMap

        clock = System.currentTimeMillis()
        for(e in data.entrySet()) {
            map = map.put(e.key, e.value)
        }
        println("FMap insert: ${System.currentTimeMillis()-clock}")
        assertEquals 500000, map.size()

        clock = System.currentTimeMillis()
        for(e in data.entrySet()) {
            map[e.key]
        }
        println("FMap get: ${System.currentTimeMillis()-clock}")

        clock = System.currentTimeMillis()
        Reference<Integer> count = [0]
        for(e in map) {
          assert map[e.key] == data[(Integer)e.key]
          count++
        }
        assert count == map.size()
        println("FMap iterate: ${System.currentTimeMillis()-clock}")

        clock = System.currentTimeMillis()
        for(i in 0..<250000) {
            map = map.remove(2*i)

        }
        println("FMap remove: ${System.currentTimeMillis()-clock}")
        assertEquals 250000, map.size()
        assertEquals (-25,map [25])
    }
}