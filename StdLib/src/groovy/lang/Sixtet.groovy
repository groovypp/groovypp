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

@Typed class Sixtet<T1,T2,T3,T4,T5,T6>  implements Externalizable {
    T1 first
    T2 second
    T3 third
    T4 forth
    T5 fifth
    T6 six

    Sixtet() {}

    Sixtet(T1 first, T2 second, T3 third, T4 forth, T5 fifth, T6 six) {
      this.first = first;
      this.second = second
      this.third = third
      this.forth = forth
      this.fifth = fifth
      this.six = six
    }

    boolean equals(obj) {
      this === obj || (obj instanceof Sixtet && eq(first, ((Sixtet) obj).first) && eq(second, ((Sixtet) obj).second) && eq(third, ((Sixtet) obj).third) && eq(forth, ((Sixtet) obj).forth) && eq(fifth, ((Sixtet) obj).fifth) && eq(six, ((Sixtet) obj).six))
    }

    private boolean eq(obj1, obj2) {
      obj1 ? obj2 == null : obj1.equals(obj2)
    }

    int hashCode() {
      31*(31*(31*(31*(31 * first?.hashCode () + second?.hashCode ()) + third?.hashCode())+ forth?.hashCode()) + fifth?.hashCode()) + six?.hashCode()
    }

    String toString() {
        "[first: $first, second: $second, third: $third, forth: $forth, fifth: $fifth, six: $six]"
    }
}
