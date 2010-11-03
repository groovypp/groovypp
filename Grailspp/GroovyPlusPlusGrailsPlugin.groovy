import org.mbte.gretty.grails.GrettyArtefactHandler
import org.mbte.gretty.grails.GrettyBean
import org.mbte.gretty.grails.RedisSessionBean

class GroovyPlusPlusGrailsPlugin {
    // the plugin version
    def version = "#version"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "1.3.5 > *"
    // the other plugins this plugin depends on
    def dependsOn = [:]
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
            "grails-app/views/error.gsp"
    ]

    // TODO Fill in these fields
    def author = "Alex Tkachman"
    def authorEmail = "alex.tkachman@gmail.com"
    def title = "Groovy++ Integration"
    def description = '''\\
The plugin integrating Groovy++ and Gretty in to Grails
'''

  def artefacts = [GrettyArtefactHandler]

  def doWithSpring = {
      gretty(GrettyBean) { bean ->
          bean.singleton = true
      }

      redisSessionListenerBean(RedisSessionBean) { bean -> 
          bean.singleton = true
      }
  }

    def doWithWebDescriptor = { webXml ->

    def mappingElement = webXml.'listener'
    mappingElement[0] + {
      'listener' {
        'listener-class'(RedisSessionBean.Listener.name)
      }
    }

    mappingElement = webXml.'filter'
    mappingElement[0] + {
      'filter' {
        'filter-name'(RedisSessionBean.Filter.name)
        'filter-class'(RedisSessionBean.Filter.name)
      }
    }

    mappingElement[0] + {
      'filter-mapping' {
        'filter-name'(RedisSessionBean.Filter.name)
        'url-pattern'("/*")
      }
    }
  }
}
