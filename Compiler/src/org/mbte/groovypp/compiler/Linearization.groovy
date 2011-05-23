package org.mbte.groovypp.compiler

import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.ClassHelper

@Typed class Linearization {
    static LinkedHashSet<ClassNode> getLinearization(ClassNode classNode, LinkedHashSet<ClassNode> accumul = []) {
        if(!accumul.contains(classNode)) {
            if(classNode.interface) {
                accumul << ClassHelper.OBJECT_TYPE
            }
            else {
                getLinearization(classNode.superClass.redirect(), accumul)
            }

            for(i in classNode.getInterfaces()) {
                getLinearization(i.redirect(), accumul)
            }

            accumul << classNode
        }

        accumul
    }
}
