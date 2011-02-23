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
@Typed package actors

import groovypp.concurrent.Agent
import groovypp.concurrent.FVector
import java.util.concurrent.CountDownLatch
import groovypp.concurrent.ContinuableJob

testWithFixedPool(20) {
//    def printer = new Agent(null,pool)
//    printer.addListener { oldValue, newValue ->
//       println newValue
//    }
//
//    CountDownLatch initCdl = [1]
//
//    Agent<FVector<Integer>> sharedVector = [FVector.emptyVector, pool]
//
//    ContinuableJob.job(pool) {
//      for(i in 0..<100) {
//        sharedVector { value ->
//            def newValue = value.length < 100*100 ? value + value.length : value
//            if (newValue.length < 100*100)
//                printer{ "Thread ${Thread.currentThread().id} ${newValue.length} ${newValue[-1]}" }
//                sharedVector(this){ println '*'}
//            newValue
//        } {
//          printer{ "Job $i finished" }
//        }
//      }
//    } {
//      initCdl.countDown()
//    }
//
//    initCdl.await()
//
//    assert sharedVector.get().length == 100*100
//    assert sharedVector.get().asList() == (0..<100*100)
}
