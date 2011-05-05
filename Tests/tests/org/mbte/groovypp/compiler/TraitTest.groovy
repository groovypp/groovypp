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





package org.mbte.groovypp.compiler

public class TraitTest extends GroovyShellTestCase {
    void testSimple() {
        shell.evaluate """
            @Trait class X {
                def getX () {
                   "x"
                }

                abstract def getY ()
            }

            @Trait class Y extends X {
                def getY () {
                   "y"
                }

                def getX () {
                   "xx"
                }
            }

            abstract class Z implements X, Y {
            }

            class ZZ extends Z {
            }

            def z = new ZZ ()
            println z.class
            assert z.getX () == 'xx' && z.getY () == 'y'
        """
    }

    void testWithFields() {
        shell.evaluate """
            @Trait class WithCoord<Own extends WithCoord> {
                int x, y

                Own moveTo (int xx, int yy) {
                   x = xx
                   y = yy
                   (Own)this
                }
            }

            @Trait class WithSize<Own extends WithCoord> {
                int h, w

                Own resize (int hh, int ww) {
                   h = hh
                   w = ww
                   (Own)this
                }
            }

            @Trait class WithRadius<Own extends WithCoord> {
                int r
            }

            class Rect implements WithCoord<Rect>, WithSize<Rect> {
            }

            class Circle implements WithCoord<Circle>, WithRadius<Circle> {
            }

            def z = new Rect ().resize(5, 5).moveTo(10,10)
            assert z.x == 10 && z.y == 10 && z.h == 5 && z.w == 5
        """
    }

    void testWithGetterSetter () {
        shell.evaluate """
@Typed package p

@Trait class Tr<T> {
        T prop

        T getProp () {
            "prop: " + this.prop
        }

        void setProp (T prop) {
            this.prop = prop
        }
}

Tr<String> o = [prop: "123"]
assert o.prop == "prop: 123"
        """
    }

    void testClosure () {
        shell.evaluate """
@Typed package p

@Trait class Tr<T> {
        List prop = []
}

abstract class Der implements Tr {
    abstract def y (int a)
}

def x(int a, Der d){
   d.y(a)
}

assert x(10) { a -> def b = 2*a; prop << b } == [20]
        """
    }

    void testStaticMethod () {
        shell.evaluate """
@Typed package p

@Trait class Tr {
    static def doIt(Object self){ self?.toString()?.toUpperCase() }
}

class Der implements Tr {
    def a () {
        "oh my god".doIt ()
    }
}

assert new Der ().a() == "OH MY GOD"
assert Der.doIt("mama mia") == "MAMA MIA"
assert Tr.doIt("mama mia") == "MAMA MIA"
        """
    }

    void testForDoc () {
        shell.evaluate """
@Typed package p

@Trait class NonRemovingIteraror<T> implements Iterator<T>{
    void remove () {
        throw new UnsupportedOperationException("remove() does not supported by iterator")
    }
}

@Trait abstract class Indexable<T> implements Iterable<T> {
      abstract int size ()

      abstract T get(int index)
      abstract void set(int index, T value)

      T getAt(int index) { get(index) }

      int getLength () { size() }

      void putAt(int index, T value) { set(index, value) }

      Iterator<T> iterator (){
        (NonRemovingIteraror)[
            curIndex: 0,
            hasNext: { curIndex < length },
            next: { get(curIndex++) }
        ]
      }
}

class ArrayWrapper<T> implements Indexable<T>{
    private T [] array

    ArrayWrapper(T[] array){
        this.array = array
    }

    T get(int index){ array[index] }

    void set(int index, T value){ array[index] = value }

    int size () { array.length }
}

def wrapper = new ArrayWrapper (new String[5])
wrapper [3] = "mama mia"
assert wrapper[3] == "mama mia"

for(e in wrapper){
    println e?.toUpperCase ()
}

class ListWrapper<T> implements Indexable<T> {
    private List<T> list

    ListWrapper(List<T> list){
        this.list = list
    }

    T get(int index){ list[index] }

    void set(int index, T value){ list[index] = value }

    int size () { list.size() }
}

def lwrapper = new ListWrapper (['mama mia'])
wrapper [0] = "mama mia"
assert wrapper[0] == "mama mia"
        """
    }
}