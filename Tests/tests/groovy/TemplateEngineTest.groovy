@Typed package groovy

import groovy.text.SimpleTemplateEngine
import groovy.text.Template
import groovypp.text.GppSimpleTemplateEngine
import groovypp.text.FastStringWriter

class TemplateEngineTest extends GroovyShellTestCase {
    static List<Pair> users = []
    static {
        for(i in 0..<1000)
          users.add(new Pair("User $i", "$i USD"))
    }

    void testNormal () {
        SimpleTemplateEngine engine = []
        def template = engine.createTemplate("""
<html>
    <body>
        <table>
        <% for(product in products) { %>
            <tr>\
                <td><%=product.code%></td>
                <td><%=product.name%></td>\
                <td><%=product.description%></td>
                <td><%=product.price%></td>\
            </tr>
        <% }%>
        </table>
    </body>
</html>
        """)

        def start = System.nanoTime()
        for(i in 0..<1000) {
            template.make([products: ProductService.products]).writeTo(new FastStringWriter())
        }
        println((System.nanoTime()-start)/(1000*1000000d))
    }

    void testNormalRecompile () {
        def sum = 0d
        for(i in 0..<100) {
            def start = System.nanoTime()
            SimpleTemplateEngine engine = []
            def template = engine.createTemplate("""
<html>
    <body>
        <table>
        <% for(product in products) { %>
            <tr>\
                <td><%=product.code%></td>
                <td><%=product.name%></td>\
                <td><%=product.description%></td>
                <td><%=product.price%></td>\
            </tr>
        <% }%>
        </table>
    </body>
</html>
            """)

            template.make([products: ProductService.products]).writeTo(new StringWriter())

            def val = ((System.nanoTime() - start)/1000000d)
            sum += val
//            println "DR $i: $val"
        }
        println(sum/100d)
    }

    void testTyped () {
        GppSimpleTemplateEngine engine = [this.class.classLoader]
        def template = engine.createTemplate("""
<%
    import groovy.Product

    List<Product> products = binding.variables.products
%>
<html>
    <body>
        <table>
        <% for(product in products) { %>
            <tr>\
                <td><%=product.code%></td>
                <td><%=product.name%></td>\
                <td><%=product.description%></td>
                <td><%=product.price%></td>\
            </tr>
        <% }%>
        </table>
    </body>
</html>
        """)

        def start = System.nanoTime()
        for(i in 0..<1000) {
            template.make([products: ProductService.products]).writeTo(new FastStringWriter())
        }
        println((System.nanoTime()-start)/(1000*1000000d))
    }

    void testTypedRecompile () {
        def sum = 0d
        for(i in 0..<100) {
            def start = System.nanoTime()
            GppSimpleTemplateEngine engine = []
            def template = engine.createTemplate("""
<%
    import groovy.Product

    List<Product> products = binding.variables.products
%>
<html>
    <body>
        <table>
        <% for(product in products) { %>
            <tr>\
                <td><%=product.code%></td>
                <td><%=product.name%></td>\
                <td><%=product.description%></td>
                <td><%=product.price%></td>\
            </tr>
        <% }%>
        </table>
    </body>
</html>
            """)

            template.make([products: ProductService.products]).writeTo(new StringWriter())

            def val = ((System.nanoTime() - start)/1000000d)
            sum += val
//            println "TR $i: $val"
        }
        println(sum/100d)
    }
}

class Product {
    String code, name, description
    int price
}

class ProductService {
    static List<Product> products = []

    static {
        for(i in 0..<1000)
            products << [code: i, name: "PROD%i", description: "Super product $i", price: i]
    }
}
