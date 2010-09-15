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

@Typed package org.mbte.gretty.redis

import java.util.concurrent.CountDownLatch

def TOTAL_OPERATIONS = 100000

RedisClient redis = [new InetSocketAddress('localhost',6379)]
redis.connect ()

def begin = Calendar.getInstance().getTimeInMillis();

//CountDownLatch cdl = [TOTAL_OPERATIONS]
for(n in 0..<TOTAL_OPERATIONS) {
    def key = "oof" + n
    redis.set(key, "rab" + n)
    redis.get(key).get()
}
//cdl.await()

def elapsed = Calendar.getInstance().getTimeInMillis() - begin;


System.out.println(((1000 * 2 * TOTAL_OPERATIONS) / elapsed) + " ops");

redis.disconnect();

