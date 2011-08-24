package org.mbte.groovypp.compiler.Issues

class Issue391Test extends GroovyShellTestCase{
    void testMe () {
        shell.evaluate """
@Typed package p

class A {
}

def u =  [] as A
println u
        """
    }
}
