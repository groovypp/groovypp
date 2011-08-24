package org.mbte.groovypp.compiler.Issues

@Typed class Issue396Test extends GroovyShellTestCase {
    void testMe () {
        shell.evaluate """
@Typed package p

interface Foo {
  Function1<String, String> x = { it.substring(1) }
}

class Bar {
  static Function1<String, String> x = { it.substring(1) }
}

assert Bar.x('mama') == 'ama'

assert Foo.x('mama') == 'ama'
"""
    }
}
