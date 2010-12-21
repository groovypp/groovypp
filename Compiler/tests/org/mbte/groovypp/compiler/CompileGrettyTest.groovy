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

@Typed package org.mbte.groovypp.compiler

import org.codehaus.groovy.tools.FileSystemCompiler
import org.codehaus.groovy.control.CompilerConfiguration
import groovy.util.test.GroovyFileSystemCompilerTestCase

class CompileGrettyTest extends GroovyFileSystemCompilerTestCase {

  CompileGrettyTest () {
    def ff = new File("../gretty/src")
    if(ff.exists() && ff.directory) {
        def file = new File("Gretty/lib")
        additionalClasspath = ""
        def files = file.listFiles()
        for(f in files) {
            if(f.name.endsWith(".jar"))
                additionalClasspath = additionalClasspath  + f + ":"
        }
    }
  }

  void testCompile () {
    def file = new File("../gretty/src")
    if(file.exists() && file.directory) {
      def finder = new FileNameFinder()
      def srcDir = file.absolutePath

      def names = finder.getFileNames(srcDir, "**/*.groovy")
      names.addAll(finder.getFileNames(srcDir, "**/*.java"))

      compiler.compile (names as String[])
    }
  }
}
