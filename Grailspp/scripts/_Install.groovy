//
// This script is executed by Grails after plugin was installed to project.
// This script is a Gant script so you can use all special variables provided
// by Gant (such as 'baseDir' which points on project base dir). You can
// use 'ant' to access a global instance of AntBuilder
//
// For example you can create directory under project tree:
//
//    ant.mkdir(dir:"${basedir}/grails-app/jobs")
//

def oldg = new File("${grailsHome}/lib").listFiles( { dir, name ->
    name.startsWith("groovy") && name.endsWith(".jar") && name.contains("-all")
} as FilenameFilter ) [0]

ant.delete(file:oldg.absolutePath)

def gpp = new File("${pluginBasedir}/lib").listFiles({ dir, name ->
    name.startsWith("groovypp") && name.endsWith(".jar") && name.contains("-all")
} as FilenameFilter ) [0]
ant.move(file:gpp.absolutePath, toDir:"${grailsHome}/lib")

def replace = { filename ->
    def file = new File("${grailsHome}/$filename")
    file.text = file.text.replaceAll(oldg.name, gpp.name)
}

replace "/bin/startGrails"
replace "/bin/startGrails.bat"
replace "/conf/groovy-starter.conf"