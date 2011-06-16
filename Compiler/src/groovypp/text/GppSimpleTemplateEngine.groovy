package groovypp.text;

import org.codehaus.groovy.control.CompilationFailedException;


import groovy.text.TemplateEngine
import groovy.text.Template
import org.codehaus.groovy.control.CompilerConfiguration

import org.codehaus.groovy.util.ManagedReference
import java.util.concurrent.ConcurrentHashMap
import org.codehaus.groovy.util.ReferenceManager;

/**
 * Processes template source files substituting variables and expressions into
 * placeholders in a template source text to produce the desired output.
 * </P>
 * The template engine uses JSP style &lt;% %&gt; script and &lt;%= %&gt; expression syntax
 * or GString style expressions. The variable '<code>out</code>' is bound to the writer that the template
 * is being written to.
 * </p>
 * Frequently, the template source will be in a file but here is a simple
 * example providing the template as a string:
 * <pre>
 * def binding = [
 *     firstname : "Grace",
 *     lastname  : "Hopper",
 *     accepted  : true,
 *     title     : 'Groovy for COBOL programmers'
 * ]
 * def engine = new groovy.text.SimpleTemplateEngine()
 * def text = '''\
 * Dear &lt;%= firstname %&gt; $lastname,
 *
 * We &lt;% if (accepted) print 'are pleased' else print 'regret' %&gt; \
 * to inform you that your paper entitled
 * '$title' was ${ accepted ? 'accepted' : 'rejected' }.
 *
 * The conference committee.
 * '''
 * def template = engine.createTemplate(text).make(binding)
 * println template.toString()
 * </pre>
 * This example uses a mix of the JSP style and GString style placeholders
 * but you can typically use just one style if you wish. Running this
 * example will produce this output:
 * <pre>
 * Dear Grace Hopper,
 *
 * We are pleased to inform you that your paper entitled
 * 'Groovy for COBOL programmers' was accepted.
 *
 * The conference committee.
 * </pre>
 * The template engine can also be used as the engine for {@link groovy.servlet.TemplateServlet} by placing the
 * following in your <code>web.xml</code> file (plus a corresponding servlet-mapping element):
 * <pre>
 * &lt;servlet&gt;
 *   &lt;servlet-name&gt;SimpleTemplate&lt;/servlet-name&gt;
 *   &lt;servlet-class&gt;groovy.servlet.TemplateServlet&lt;/servlet-class&gt;
 *   &lt;init-param&gt;
 *     &lt;param-name&gt;template.engine&lt;/param-name&gt;
 *     &lt;param-value&gt;groovy.text.SimpleTemplateEngine&lt;/param-value&gt;
 *   &lt;/init-param&gt;
 * &lt;/servlet&gt;
 * </pre>
 * In this case, your template source file should be HTML with the appropriate embedded placeholders.
 *
 * @author sam
 * @author Christian Stein
 * @author Paul King
 * @author Alex Tkachman
 */
@Typed class GppSimpleTemplateEngine extends TemplateEngine {
    /**
     * @param verbose true if you want the engine to display the template source file for debugging purposes
     */
    boolean verbose

    private static int counter = 1

    protected GroovyClassLoader loader

    GppSimpleTemplateEngine() {
        this(GppSimpleTemplateEngine.classLoader)
    }

    GppSimpleTemplateEngine(ClassLoader parentLoader, String scriptClass = GppTemplateScript.name) {
        def config = new CompilerConfiguration()
        config.scriptBaseClass = scriptClass
        loader = [parentLoader, config]
    }

    Template createTemplate(Reader reader) throws CompilationFailedException, IOException {
        SimpleTemplate template = []
        try {
            def fileName = "SimpleTemplateScript" + counter++ + ".gpptl"
            template.scriptClass = loader.parseClass(new GroovyCodeSource(reader, fileName, GroovyShell.DEFAULT_CODE_BASE))
        } catch (Exception e) {
            throw new GroovyRuntimeException("Failed to parse template script (your template may contain an error or be trying to use expressions not currently supported): " + e.getMessage())
        }
        return template
    }

    public static StringReader createScriptSource (Reader reader) {
        SimpleTemplate template = []
        def parse = template.parse(reader)
//        System.err.println parse
        [parse]
    }

    protected static class SimpleTemplate implements Template {

        protected Class<GppTemplateScript> scriptClass

        private boolean open

        public Writable make(final Map map = null) {
            return new Writable() {
                /**
                 * Write the template document with the set binding applied to the writer.
                 *
                 * @see groovy.lang.Writable#writeTo(java.io.Writer)
                 */
                public Writer writeTo(Writer writer) {
                    def scriptObject = scriptClass.newInstance()[
                        binding: !map ? [] : [map],
                        out: [writer]
                    ]
                    scriptObject.run()
                    scriptObject.out.flush()
                    writer
                }

                /**
                 * Convert the template and binding into a result String.
                 *
                 * @see java.lang.Object#toString()
                 */
                public String toString() {
                    StringWriter sw = []
                    writeTo(sw)
                    sw.toString()
                }
            };
        }

        /**
         * Parse the text document looking for <% or <%= and then call out to the appropriate handler, otherwise copy the text directly
         * into the script while escaping quotes.
         *
         * @param reader a reader for the template text
         * @return the parsed text
         * @throws IOException if something goes wrong
         */
        protected String parse(Reader reader) throws IOException {
            if (!reader.markSupported()) {
                reader = new BufferedReader(reader);
            }
            StringWriter sw = []
            startScript(sw)
            int c
            while ((c = reader.read()) != -1) {
                if (c == '<') {
                    reader.mark(1)
                    c = reader.read()
                    if (c != '%') {
                        if(!open) {
                            open = true
                            sw.write('out.print(\"\"\"')
                        }
                        sw.write('<')
                        reader.reset()
                    } else {
                        reader.mark(1)
                        c = reader.read()
                        if (c == '=') {
                            groovyExpression(reader, sw)
                        } else {
                            reader.reset()
                            groovySection(reader, sw)
                        }
                    }
                    continue // at least '<' is consumed ... read next chars.
                }
                if (c == '$') {
                    reader.mark(1)
                    c = reader.read()
                    if (c != '{') {
                        if(!open) {
                            open = true
                            sw.write('out.print(\"\"\"')
                        }
                        sw.write('$')
                        reader.reset()
                    } else {
                        reader.mark(1)
                        if(!open) {
                            open = true
                            sw.write('out.print(\"\"\"')
                        }
                        sw.write('${')
                        processGSstring(reader, sw)
                    }
                    continue // at least '$' is consumed ... read next chars.
                }
                if (c == '\"') {
                    if(!open) {
                        open = true
                        sw.write('out.print(\"\"\"')
                    }
                    sw.write('\\');
                }
                /*
                 * Handle raw new line characters.
                 */
                if (c == '\n' || c == '\r') {
                    if (c == '\r') { // on Windows, "\r\n" is a new line.
                        reader.mark(1)
                        c = reader.read()
                        if (c != '\n') {
                            reader.reset()
                        }
                    }
                    if(!open) {
                        open = true
                        sw.write('out.print(\"\"\"')
                    }
                    sw.write("\n")
                    continue;
                }
                if(!open) {
                    open = true
                    sw.write('out.print(\"\"\"')
                }
                sw.write(c)
            }
            endScript(sw);
            return sw.toString();
        }

        private void startScript(StringWriter sw) {
            sw.write("/* Generated by GppSimpleTemplateEngine */\n");
        }

        private void endScript(StringWriter sw) {
            if(open) {
                sw.write("\"\"\");\n");
                open = false
            }
        }

        private void processGSstring(Reader reader, StringWriter sw) throws IOException {
            int c;
            while ((c = reader.read()) != -1) {
                if (c != '\n' && c != '\r') {
                    sw.write(c);
                }
                if (c == '}') {
                    break;
                }
            }
        }

        /**
         * Closes the currently open write and writes out the following text as a GString expression until it reaches an end %>.
         *
         * @param reader a reader for the template text
         * @param sw     a StringWriter to write expression content
         * @throws IOException if something goes wrong
         */
        private void groovyExpression(Reader reader, StringWriter sw) throws IOException {
            if(open)
                sw.write("\"\"\"); \nout.print(");
            else
                sw.write("\nout.print(");
            open = false

            int c;
            while ((c = reader.read()) != -1) {
                if (c == '%') {
                    c = reader.read();
                    if (c != '>') {
                        sw.write('%');
                    } else {
                        break;
                    }
                }
                if (c != '\n' && c != '\r') {
                    sw.write(c);
                }
            }
            sw.write(");\nout.print(\"\"\"");
            open = true
        }

        /**
         * Closes the currently open write and writes the following text as normal Groovy script code until it reaches an end %>.
         *
         * @param reader a reader for the template text
         * @param sw     a StringWriter to write expression content
         * @throws IOException if something goes wrong
         */
        private void groovySection(Reader reader, StringWriter sw) throws IOException {
            if(open)
                sw.write("\"\"\");");
            open = false

            int c;
            while ((c = reader.read()) != -1) {
                if (c == '%') {
                    c = reader.read();
                    if (c != '>') {
                        sw.write('%');
                    } else {
                        break;
                    }
                }
                /* Don't eat EOL chars in sections - as they are valid instruction separators.
                 * See http://jira.codehaus.org/browse/GROOVY-980
                 */
                // if (c != '\n' && c != '\r') {
                sw.write(c);
                //}
            }
            sw.write(";\nout.print(\"\"\"");
            open = true
        }

    }

    class CacheEntry extends ManagedReference<Class> {
        final File path

        CacheEntry(File path, Class cls) {
            super(ReferenceManager.getDefaultSoftBundle(), cls)
            this.path = path
        }

        void finalizeReference() {
            cache.remove(path, this)
            super.finalizeReference()
        }
    }

    protected final ConcurrentHashMap cache = []

    protected Class<GppTemplateScript> compile(File file) {
        SimpleTemplate template = createTemplate(file)
        cache.put(file, new CacheEntry(file, template.scriptClass))
        template.scriptClass
    }

    Class<GppTemplateScript> getTemplateClass(File file) {
        file = file.canonicalFile
        CacheEntry gotEntry = cache.get(file)
        Class got = gotEntry?.get()
        if(!got) {
            if(!file.exists())
                throw new IOException("No such file $file")

            if(file.directory)
                throw new IOException("File $file is directory")

            SimpleTemplate template = createTemplate(file)
            cache.put(file, new CacheEntry(file, template.scriptClass))
            got = compile(file)
        }
        got
    }
}
