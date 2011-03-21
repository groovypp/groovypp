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
package groovy.lang

@Typed class Quintet<T1,T2,T3,T4,T5>  implements Externalizable {
    T1 first
    T2 second
    T3 third
    T4 forth
    T5 fifth

    Quintet() {}

    Quintet(T1 first, T2 second, T3 third, T4 forth, T5 fifth) {
      this.first = first;
      this.second = second
      this.third = third
      this.forth = forth
      this.fifth = fifth
    }

    boolean equals(obj) {
      this === obj || (obj instanceof Quintet && eq(first, ((Quintet) obj).first) && eq(second, ((Quintet) obj).second) && eq(third, ((Quintet) obj).third) && eq(forth, ((Quintet) obj).forth)  && eq(fifth, ((Quintet) obj).fifth))
    }

    private boolean eq(obj1, obj2) {
      obj1 ? obj2 == null : obj1.equals(obj2)
    }

    int hashCode() {
      31*(31*(31*(31 * first?.hashCode () + second?.hashCode ()) + third?.hashCode())+ forth?.hashCode()) + fifth?.hashCode()
    }

    String toString() {
        "[first: $first, second: $second, third: $third, forth: $forth, fifth: $fifth]"
    }
}
