package org.mbte.groovypp.compiler

public class ClosureTest extends GroovyShellTestCase {

  void testAssignable () {
      def res = shell.evaluate("""
      interface I<T> {
        T calc ()
      }

      @Typed
      def u () {
        I r = {
            11
        }

        def c = (I){
            12
        }

        def u = { 13 } as I

        [r.calc (), c.calc (), u.calc()]
      }

      u ()
  """)
      assertEquals ([11, 12, 13], res)
  }
    
    void testListAsArray () {
        def res = shell.evaluate("""
        interface I<T> {
          T calc ()
        }

        @Typed
        def u () {
          def x = (I[])[ {->10}, {->9} ]

          [x[0].calc(), x[1].calc () ]
        }

        u ()
    """)
        assertEquals ([10, 9], res)
    }

    void testMap () {
        def res = shell.evaluate("""
        interface I<T> {
          T calc ()
        }

        class ListOfI extends LinkedList<I> {}

        @Typed(debug=true)
        def u () {
          def     x = [{14}] as List<I>
          def     y = (List<I>)[{15}]
          ListOfI z = [{16}]

          [x[0].calc (), y[0].calc(), z[0].calc () ]
        }

        u ()
    """)
        assertEquals ([14, 15, 16], res)
    }

  void testClosure() {
    shell.evaluate("""
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Typed
class MailBox {
    private static final ExecutorService executor = Executors.newCachedThreadPool();

    protected final void loop (TypedClosure<MailBox> operation) {
        executor.submit {
            operation.setDelegate this
            operation ()
            loop (operation)
        }
    }
}

1
""")
  }

  void testClosureAsObj() {
    shell.evaluate("""
    import java.util.concurrent.ExecutorService
    import java.util.concurrent.Executors

    @Typed
    class MailBox {
        private static final ExecutorService executor = Executors.newCachedThreadPool();

        protected final void loop (TypedClosure<MailBox> operation) {
            executor.submit {
                operation.setDelegate this
                operation ()
                loop (operation)
            }
        }
    }

    1
    """)
  }
}