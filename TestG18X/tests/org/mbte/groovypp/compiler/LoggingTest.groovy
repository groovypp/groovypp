/*
 * Copyright 2009-2011 MBTE Sweden AB.
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
package org.mbte.groovypp.compiler

@Typed class LoggingTest extends GroovyShellTestCase {
    void testMe () {
        shell.evaluate """
@Typed package p

@Grab('org.slf4j:slf4j-api:1.6.1')
@Grab('ch.qos.logback:logback-classic:0.9.28')
import groovy.util.logging.Slf4j

@Slf4j
public class TestLog {

    public void doLog() {
        log.info("I'm in the logger")
    }

}


def t= new TestLog()
t.doLog()
        """
    }
}
