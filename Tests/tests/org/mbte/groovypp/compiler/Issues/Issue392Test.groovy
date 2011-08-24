package org.mbte.groovypp.compiler.Issues

class Issue392Test extends GroovyShellTestCase {
    void testMe () {
        shell.evaluate """
@Typed package p

def start = 0L


def strings = ['a', 'b', 'c']
def count = 100000000

start = System.currentTimeMillis()

for (i in 0..count) {
  for (c in strings) {
    if (c == 'c') {
      break
    }
  }
}

println (System.currentTimeMillis() - start)

start = System.currentTimeMillis()
for (i in 0..count) {
  Predicate1<String> cl = { it == 'c' }
  strings.find (cl)
}

println (System.currentTimeMillis() - start)

start = System.currentTimeMillis()
for (i in 0..count) {
    Predicate1<String> cl = { it == 'c' }
    def self = strings.iterator()
    while (self.hasNext()) {
        def el = self.next()
        if (cl(el))
            break
    }
}

println (System.currentTimeMillis() - start)
        """
    }
}
