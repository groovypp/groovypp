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

package groovypp.concurrent

import java.util.concurrent.*
import java.util.concurrent.locks.LockSupport

/**
 * A bit faster implementation of fixed thread pool Executor
 */
@Typed class FThreadPool implements Executor {

  private static class State {
      FQueue<Runnable> queue
      FList<Worker>  waiting
  }

  protected volatile State state = [queue:FQueue.emptyQueue, waiting:FList.emptyList]

  protected static final Runnable stopMarker = {}

  private final CountDownLatch termination

  FThreadPool(int num = Runtime.getRuntime().availableProcessors(), ThreadFactory threadFactory = Executors.defaultThreadFactory()) {
    termination = [num]
    for(i in 0..<num) {
      threadFactory.newThread(new Worker ()).start()
    }
  }

  protected final void runNormal(State s) {
    def got = s.queue.removeFirst()
    if(state.compareAndSet(s, [queue: got.second, waiting:s.waiting])) {
      got.first.run()
    }
  }

  protected final void runStopping(State s) {
    def got = s.queue.removeFirst().second.removeFirst()
    if(state.compareAndSet(s, [queue:got.second.addFirst(stopMarker), waiting:s.waiting])) {
      got.first.run()
    }
  }

  protected final void park(State s, Worker worker) {
      if(state.compareAndSet(s, [queue:FQueue.emptyQueue, waiting:s.waiting + worker])) {
          for(def spin = 2000; spin; spin--) {
              def command = worker.mailbox
              if(command) {
                  worker.mailbox = null

                  if(command === stopMarker)
                    return

                  command.run ()

                  LockSupport.park() // compensate of unpark from enqueing thread
                  return
              }
          }

          for(;;) {
              LockSupport.park()

              def command = worker.mailbox
              if(command) {
                  // already removed from waiting list
                  worker.mailbox = null

                  if(command === stopMarker)
                    return
                  
                  command.run ()
                  return
              }
              else {
                  if(Thread.interrupted())
                    throw new InterruptedException();

                  // was unparked without reason, so still in waiting list
              }
          }
      }
  }

  final void execute(Runnable command) {
    for(;;) {
      def s = state
      if (!s.queue.empty && s.queue.first == stopMarker)
        throw new RejectedExecutionException()

      if(s.waiting.empty) {
          if(state.compareAndSet(s, [queue:s.queue + command, waiting:FList.emptyList])) {
            break
          }
      }
      else {
          if(state.compareAndSet(s, [queue:FQueue.emptyQueue, waiting:s.waiting.tail])) {
            def head = s.waiting.head
            head.mailbox = command
            LockSupport.unpark(head.thread)
            break
          }
      }
    }
  }

  /**
   * Initiate process of shutdown
   * No new tasks can be scheduled after that point
   */
  void shutdown() {
    for(;;) {
      def s = state
      if (s.queue.empty) {
        if(state.compareAndSet(s, [queue: FQueue.emptyQueue + stopMarker, waiting: FList.emptyList])) {
          unparkAll(s)
          break
        }
      }
      else {
        if (s.queue.first !== stopMarker) {
          if(state.compareAndSet(s, [queue:s.queue.addFirst(stopMarker), waiting:FList.emptyList])) {
            unparkAll(s)
            break
          }
        }
        else {
            break
        }
      }
    }
  }

    private void unparkAll(State s) {
        for (w in s.waiting) {
            w.mailbox = stopMarker
            LockSupport.unpark(w.thread)
        }
    }

    /**
   * Initiate process of shutdown
   * No new tasks can be scheduled after that point and all tasks, which not started execution yet, will be unscheduled
   */
  List<Runnable> shutdownNow() {
      for(;;) {
        def s = state
        if (s.queue.empty) {
          if(state.compareAndSet(s, [queue: FQueue.emptyQueue + stopMarker, waiting: FList.emptyList])) {
            unparkAll(s)
            return []
          }
        }
        else {
          if (s.queue.first !== stopMarker) {
            if(state.compareAndSet(s, [queue:s.queue.addFirst(stopMarker), waiting:FList.emptyList])) {
              unparkAll(s)
              return s.queue.iterator().asList()
            }
          }
          else {
              return []
          }
        }
      }
  }

  boolean awaitTermination(long timeout, TimeUnit timeUnit) {
    termination.await(timeout, timeUnit)
  }

  private class Worker implements Runnable {
      volatile Thread thread
      volatile Runnable mailbox

      void run() {
          thread = Thread.currentThread()

          try {
            for(;;) {
                def s = state

                if(s.queue.empty) {
                    park(s, this)
                }
                else {
                    if(s.queue.first === stopMarker) {
                      if(s.queue.size() == 1)
                        return

                      runStopping(s)
                    }
                    else {
                      runNormal(s)
                    }
                }
            }
          }
          finally {
            termination.countDown()
          }
      }
  }
}
