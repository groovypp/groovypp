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

import java.util.concurrent.CountDownLatch
import groovypp.concurrent.ContinuableJob
import java.util.concurrent.Executor

testWithFixedPool(20) {
    println(maxRandom(pool, new double[100000], 0, 10000).get())
}

ContinuableJob<Double> maxRandom(Executor pool, double [] array, int from = 0, int to = array.length) {
//    def len = to-from-1
//    if(len < 50) {
//      def max = Double.MIN_VALUE
//      for(int i=from; i != to; i++) {
//        def value = Math.random() * 1000
//        array[i] = value
//        if(value > max)
//           max = value
//      }
//
//      ContinuableJob.doneJob(max)
//    }
//    else {
//      pool.job {
//        def splitPoint = from + (len >> 1)
//
//        def first  = maxRandom(pool, array, from, splitPoint)
//        def second = maxRandom(pool, array, splitPoint, to)
//
//        whenChildrenReady: {
//          set(Math.max(first.get(), second.get()))
//        }
//      }
//    }
}