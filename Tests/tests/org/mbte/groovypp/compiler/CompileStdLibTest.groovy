/*
 * Copyright 2009-2011 MBTE Sweden AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

class CompileStdLibTest extends GroovyFileSystemCompilerTestCase {
  void testMe () {
    assertNotNull compiler

    def finder = new FileNameFinder()
    def srcDir = "../StdLib/src/"

    assertTrue new File(srcDir).exists()

    def names = finder.getFileNames(srcDir, "**/*.groovy")
    names.addAll(finder.getFileNames(srcDir, "**/*.java"))

    assertFalse names.empty

    compiler.compile (names as String[])
  }
}

