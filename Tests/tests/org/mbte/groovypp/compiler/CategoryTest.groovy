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

class CategoryTest extends GroovyShellTestCase {
    void testSelfMethod () {
        shell.evaluate """
@Typed package p

class Category {
    static String sizeAsString(List self){
        self?.size()
    }

    def doTest () {
        assert [].sizeAsString () == "0"
        assert [239, 932].sizeAsString () == "2"
        assert ((List)null).sizeAsString () == "0"
    }
}

new Category().doTest ()
        """
    }

    void testInheritedMethod () {
        shell.evaluate """
@Typed package p

class Category {
    static String sizeAsString(List self){
        self?.size()
    }
}

class Category2 extends Category {
    def doTest () {
        assert [].sizeAsString () == "0"
        assert [239, 932].sizeAsString () == "2"
        assert ((List)null).sizeAsString () == "0"
    }
}

new Category2().doTest ()
        """
    }

    void testUseClass () {
        shell.evaluate """
@Typed package p

class Category {
    static String sizeAsString(List self){
        self?.size()
    }
}

@Use(Category)
class Category2{
    def doTest () {
        assert [].sizeAsString () == "0"
        assert [239, 932].sizeAsString () == "2"
        assert ((List)null).sizeAsString () == "0"
    }
}

new Category2().doTest ()
        """
    }

    void testUseInheritedClass () {
        shell.evaluate """
@Typed package p

class Category {
    static String sizeAsString(List self){
        self?.size()
    }
}

@Use(Category)
class Category2{

}

class Category3 extends Category2 {
    def doTest () {
        assert [].sizeAsString () == "0"
        assert [239, 932].sizeAsString () == "2"
        assert ((List)null).sizeAsString () == "0"
    }
}

new Category3().doTest ()
        """
    }

    void testOuter () {
        shell.evaluate """
@Typed package p

class Category {
    static String sizeAsString(List self){
        self?.size()
    }

    static class Tester {
        def doTest () {
            assert [].sizeAsString () == "0"
            assert [239, 932].sizeAsString () == "2"
            assert ((List)null).sizeAsString () == "0"
        }
    }
}

new Category.Tester().doTest ()
        """
    }

    void testUseMethod () {
//        shell.evaluate """
//@Typed package p
//
//class Category {
//    static String sizeAsString(List self){
//        self?.size()
//    }
//}
//
//class Category2{
//
//    @Use(Category)
//    def doTest () {
//        assert [].sizeAsString () == "0"
//        assert [239, 932].sizeAsString () == "2"
//        assert ((List)null).sizeAsString () == "0"
//    }
//}
//
//new Category2().doTest ()
//        """
    }


    void testUseTime () {
      shell.evaluate """
@Typed package p
import groovy.time.TimeCategory
import groovy.time.TimeDuration
@Use(TimeCategory)
class A{
  void doIt () {
    def now = new Date()
    def twoMonthsAgo = now - (new Integer(2)).months
    assert 10.millisecond instanceof TimeDuration
  }
}
  new A().doIt ()
      """
    }
}
