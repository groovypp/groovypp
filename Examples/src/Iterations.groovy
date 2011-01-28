
/*
 * Copyright 2009-2010 MBTE Sweden AB.
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
// @Typed
package test

class Chain
{
    int size
    Person first

    def init(siz)
    {
        Person last
        size = siz
        for(def i = 0 ; i < siz ; i++)
        {
            def current = new Person()
            current.count = i
            if (i == 0) first = current
            if (last != null)
            {
                last.next = current
            }
            current.prev = last
            last = current
        }
        first.prev = last
        last.next = first
    }

    def kill(int nth)
    {
        def current = first
        int shout = 1
        while(current.next != current)
        {
            shout = current.shout(shout,nth)
            current = current.next
        }
        first = current
    }
}

class Person
{
    int count
    Person prev
    Person next

    int shout(int shout,int deadif)
    {
        if (shout < deadif)
        {
            return (shout + 1)
        }
        prev.next = next
        next.prev = prev
        return 1
    }
}

println "Starting"
def ITER = 100000
def start = System.nanoTime()
for(def i = 0 ; i < ITER ; i++)
{
    def chain = new Chain()
    chain.init(40)
    chain.kill(3)
}
def end = System.nanoTime()
println "Total time = " + ((end - start)/(ITER * 1000)) + " microseconds"
