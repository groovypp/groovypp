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

package groovypp.channels

import groovypp.concurrent.FQueue

@Typed abstract class SchedulingChannel<M> extends MessageChannel<M> implements Runnable {
    protected volatile FQueue<M> queue = FQueue.emptyQueue

    void post(M message) {
        for (;;) {
            def q = queue
            def newQ = queue.addLast(message)
            if (queue.compareAndSet(q, newQ)) {
                if (q.size() < concurrencyLevel)
                    schedule ()
                return
            }
        }
    }

    void run () {
        for (;;) {
            def q = queue
            def newQ = queue.removeFirst()
            if (queue.compareAndSet(q, newQ.second)) {
                onMessage(newQ.first)
                if (q.size() > concurrencyLevel)
                    schedule ()
                return
            }
        }
    }

    abstract void onMessage(M message)

    abstract void schedule ()

    protected int getConcurrencyLevel () { 1 }
}
