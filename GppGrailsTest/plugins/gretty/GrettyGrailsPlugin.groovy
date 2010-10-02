import org.mbte.gretty.grails.GrettyArtefactHandler
import org.mbte.gretty.grails.GrettyBean

class GrettyGrailsPlugin {
    // the plugin version
    def version = "0.1"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "1.3.4 > *"
    // the other plugins this plugin depends on
    def dependsOn = [:]
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
            "grails-app/views/error.gsp"
    ]

    def artifacts = [GrettyArtefactHandler]

    // TODO Fill in these fields
    def author = "Alex Tkachman"
    def authorEmail = "alex.tkachman@gmail.com"
    def title = "Gretty Integration"
    def description = '''\\
The plugin integrating Gretty in to Grails
'''

    def doWithSpring = {
        gretty(GrettyBean) { bean ->
            bean.singleton = true
        }
    }
}
