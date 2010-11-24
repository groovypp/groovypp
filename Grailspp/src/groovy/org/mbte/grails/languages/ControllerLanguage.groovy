/*
 * Copyright 2009-2010 MBTE Sweden AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mbte.grails.languages

import org.mbte.groovypp.compiler.languages.LanguageDefinition
import org.mbte.grails.compiler.GrailsScriptLanguageProvider

@Typed class ControllerLanguage extends GrailsLanguage {

    ControllerLanguage() {
        super(GrailsScriptLanguageProvider.CONTROLLERS_ANCHOR)

        def oldConversion = conversion
        conversion = { moduleNode ->
            moduleNode.classes[0]
            oldConversion.execute moduleNode
        }
    }
}