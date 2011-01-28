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
@Typed package actors

import groovypp.channels.ExecutingChannel
import java.util.concurrent.Executors
import java.util.concurrent.CountDownLatch

def n = 20

CountDownLatch completed = [n]

class Shop extends ExecutingChannel {
    Random random = []

    private volatile int queueSize

    void visit(int customerId, CountDownLatch completed) {
        println "Customer $customerId entering the shop"
        for(;;) {
            def qs = queueSize
            if(qs <= 3){
                if(queueSize.compareAndSet(qs, qs+1)) {
                    println "Customer $customerId taking a seat"
                    schedule {
                        println "Starting hair cut of customer $customerId"
                        Thread.sleep (100 + random.nextInt(400))
                        println "Customer $customerId got hair cut"
                        queueSize.decrementAndGet()
                        completed.countDown()
                    }
                    return
                }
            }
            else {
                println "Customer $customerId leaving the shop. No free seats"
                completed.countDown()
                return
            }
        }
    }
}

def shop = new Shop (executor: Executors.newSingleThreadExecutor())

def executor = Executors.newFixedThreadPool(Runtime.runtime.availableProcessors())

(0..<n).each { id ->
    executor.execute {
        Thread.sleep (100 + shop.random.nextInt(600))
        shop.visit id, completed
    }
}

completed.await()
System.exit(0)