package groovypp.text

@Typed abstract class GppTemplateScript extends Script {
    protected FastPrintWriter out

    void writeTo(Map map = null, Writer writer) {
        binding = !map ? [] : [map]
        out =  writer instanceof FastPrintWriter ? writer : [writer]
        run()
        out.flush()
    }
}
