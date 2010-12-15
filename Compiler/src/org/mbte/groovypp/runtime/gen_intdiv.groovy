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





package org.mbte.groovypp.runtime

def numbers = ['byte', 'short', 'int', 'long', 'Byte', 'Short', 'Integer', 'Long']

numbers.each { a ->
  numbers.each { b ->
    def res = (a == 'long' || b == 'long' || a == 'Long' || b == 'Long') ? 'long' : 'int'
    println """
  public static $res intdiv($a left, $b right) {
      return (($res)left) / (($res)right);
  }
    """
  }
}

numbers.each { a ->
  println "$a v_$a = 5"
  numbers.each { b ->
    def res = (a == 'long' || b == 'long' || a == 'Long' || b == 'Long') ? 'long' : 'int'
    println """
      $b v_${a}_$b = 2
      assert v_${a}.intdiv(v_${a}_$b).class == $res
    """
  }
}