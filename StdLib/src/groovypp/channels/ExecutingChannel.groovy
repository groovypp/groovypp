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

package groovypp.channels

import java.util.concurrent.Executor

import groovypp.concurrent.FQueue
import groovypp.concurrent.CallLater

/**
 * Channel, which asynchronously schedule incoming messages for processing.
 * No more than one message processed at any given moment
 */
@Typed abstract class ExecutingChannel<M> extends MessageChannel<M> implements Runnable {
    protected volatile FQueue<M> queue = FQueue.emptyQueue

    /**
     * non volatile. should be effectively final
     */
    Executor executor

    /**
     * non volatile. should be effectively final
     */
    boolean  runFair

    /**
     * Special tag saying that processing thread(reader) is processing last message in the queue.
     * This is kind of protocol between writers to QueuedChannel and reader.
     */
    protected static final FQueue busyEmptyQueue = FQueue.emptyQueue + null

    final void post(M message) {
        for (;;) {
            def oldQueue = queue
            def newQueue = (oldQueue === busyEmptyQueue ? FQueue.emptyQueue : oldQueue).addLast(message)
            if (queue.compareAndSet(oldQueue, newQueue)) {
                if(oldQueue.empty)
                    executor.execute(this)
                return
            }
        }
    }

    final void postFirst(M message) {
        for (;;) {
            def oldQueue = queue
            def newQueue = (oldQueue === busyEmptyQueue ? FQueue.emptyQueue : oldQueue).addFirst(message)
            if (queue.compareAndSet(oldQueue, newQueue)) {
                if(oldQueue.empty)
                    executor.execute(this)
                return
            }
        }
    }

    final void run() {
        runFair ? runFair () : runNonfair ()
    }

    private void runFair () {
        for (;;) {
            def q = queue
            def removed = q.removeFirst()
            if (q.size() == 1) {
                if (queue.compareAndSet(q, busyEmptyQueue)) {
                    onMessage removed.first
                    if (!queue.compareAndSet(busyEmptyQueue, FQueue.emptyQueue)) {
                        executor.execute this
                    }
                    return
                }
            }
            else {
                if (queue.compareAndSet(q, removed.second)) {
                    onMessage removed.first
                    executor.execute this
                    return
                }
            }
        }
    }

    private void runNonfair () {
        for (;;) {
            def q = queue
            if (queue.compareAndSet(q, busyEmptyQueue)) {
                for(m in q) {
                    onMessage m
                }
                if(!queue.compareAndSet(busyEmptyQueue, FQueue.emptyQueue)) {
                    continue
                }
                break
            }
        }
    }

    protected void onMessage(M message) {
        if(message instanceof ExecuteCommand) {
            ((ExecuteCommand)message).run ()
        }
    }


    final <S> ExecuteCommand<S> schedule (ExecuteCommand<S> command) {
        post(command)
        command
    }

    abstract static class ExecuteCommand<S> extends CallLater<S> {}
}