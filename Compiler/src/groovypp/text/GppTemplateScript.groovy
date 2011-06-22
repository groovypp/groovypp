package groovypp.text

@Typed abstract class GppTemplateScript extends Script {
    FastPrintWriter out

    void writeTo(Map map = null, Writer writer) {
        binding = !map ? [] : [map]
        out =  writer instanceof FastPrintWriter ? writer : [writer]
        run()
        out.flush()
    }

    void println() {
        out.println()
    }

    void print(Object value) {
        out.print(value)
    }

    void println(Object value) {
        out.println(value)
    }
}
