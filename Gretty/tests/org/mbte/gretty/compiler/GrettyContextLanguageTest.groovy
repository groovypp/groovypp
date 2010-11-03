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
package org.mbte.gretty.compiler

@Typed class GrettyContextLanguageTest extends GroovyShellTestCase {
    void testMe () {
        Class<GrettyContextProvider> res = shell.classLoader.parseClass("""\
scriptLanguage: org.mbte.gretty.compiler.GrettyContextLanguage

webContexts: [
    "/websockets": [
            static: 'abcd'
        ]
]
""")

        assert res.newInstance().webContexts['/websockets'].'static' == 'abcd'
        assert res.package == null
    }

    void testGrails () {
        Class<GrettyContextProvider> res = shell.classLoader.parseClass("""\
webContexts: [
    "/websockets": [
            static: 'abcd'
        ]
]
""", "/Development/grails-app/gretty/somepackage/script${System.currentTimeMillis()}.groovy")

        println res.name
        assert res.newInstance().webContexts['/websockets'].'static' == 'abcd'
        assert res.package.name == 'somepackage'
    }
}
