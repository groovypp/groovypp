grails.project.dependency.resolution = {
    inherits "global"
    
    dependencies {
        provided group:'redis.clients', name:'jedis', version:'1.3.1'
        provided group:'org.codehaus.jackson', name:'jackson-mapper-asl', version:'1.6.1'
        provided group:'org.freemarker', name:'freemarker', version:'2.3.16'
        provided group:'org.slf4j', name:'slf4j-log4j12', version:'1.6.1'
    }
}