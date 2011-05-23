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
@Typed package p

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


    void testFieldInClosure () {
        shell.evaluate """
    @Typed package p

    Callable u = {
        @Field def f1, f2

        preStart: {
            f1 = "aaa"
        }
        postStop: {
            f2 = "bbb"
        }

        "\${preStart ()} \${postStop()} \$f1 \$f2"
    }

    assert u() == 'aaa bbb aaa bbb'
            """
    }

    void testExample () {
        shell.evaluate """
@Typed package p

abstract class MessageHandler {
    abstract void onMessage(msg)

    final void leftShift(msg){
        onMessage(msg)
    }
}

MessageHandler actor1 = {msg ->
    println msg
}
actor1 << "Hello, world!"

abstract class StartupAwareMessageHandler extends MessageHandler{
    protected void doStart (String args) {}

    final MessageHandler start(String args) {
        doStart(args)
        this
    }
}

StartupAwareMessageHandler actor2 = { msg ->
    @Field boolean upper

    doStart: { args ->
        upper = args.toUpperCase () == 'TRUE'
    }

    println upper ? msg.toString().toUpperCase() : msg
}

actor2.start (new Random().nextBoolean().toString()) << "Hello, world!"
        """
    }
}
