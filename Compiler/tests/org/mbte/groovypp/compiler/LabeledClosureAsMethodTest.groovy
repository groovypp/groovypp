/*
 * Copyright 2009-2011 MBTE Sweden AB.
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
package org.mbte.groovypp.compiler

class LabeledClosureAsMethodTest extends GroovyShellTestCase {
  void testAbstract () {
    shell.evaluate """
@Typed(debug=true) package p

abstract class Listener {
  abstract int call ()

  int onReady (int value) {
  }

  void onAllReady () {
  }

  int doIt () {
    onReady(call())
  }
}

Listener l = {
  onReady: { value ->
    5 * value
  }

  onAllReady: { ->
  }

  10
}

assert l.doIt() == 50
    """
  }


  void testScript () {
    shell.evaluate """
@Typed package p

sqrt: {
  16
}

sqrt: {
  64
}

sqrt: { double x ->
  Math.sqrt(x)
}

def var = sqrt()
println var
assert var.class == Integer.class
def var1 = sqrt(var)
println var1
assert var1.class == Double.class
assert var1 == 8

    """
  }
}
