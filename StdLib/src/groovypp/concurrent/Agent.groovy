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

package groovypp.concurrent

import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicInteger

/**
 *
 * @param < T > type of referenced data
 */
@Typed class Agent<T> extends AbstractRef<T> implements Runnable {
  private static class PendingCall {
    Agent agent
    Operation operation
  }

  private static class ErrorValue {
    Throwable error
  }

  private static final ThreadLocal<FList<PendingCall>> pendingCalls = []

  abstract static class Operation<T> implements Cloneable {
    abstract T call(T value)

    private Runnable andContinue

    Object clone() {
      super.clone()
    }
  }

  private volatile Object value

  protected volatile FQueue<Operation<T>> queue = FQueue.emptyQueue

  final Executor executor

  Agent(T ref = null, Executor executor) {
    this.value = ref
    this.executor = executor
  }

  final T get() {
    checkError()
  }

  protected static final FQueue busyEmptyQueue = FQueue.emptyQueue + null

  final void set(T newValue) {
    this.call { oldValue ->
      newValue
    }
  }

  final void call(Operation<T> operation, Runnable andContinue = null) {
    checkError()

    operation = operation.clone ()
    operation.andContinue = andContinue
    def pending = pendingCalls.get()
    if (pending != null) {
      pendingCalls.set(pending + [agent: this, operation: operation])
    }
    else {
      schedule(operation)
    }
  }

  private T checkError() {
    def currentValue = value
    if (currentValue instanceof ErrorValue) {
      if (currentValue.error instanceof RuntimeException)
        throw currentValue.error

      throw new IllegalStateException("Agent has failed and need reset", currentValue.error)
    }
    currentValue
  }

  private void schedule(Operation<T> operation) {
    for (;;) {
      def oldQueue = queue
      def newQueue = (oldQueue === busyEmptyQueue ? FQueue.emptyQueue : oldQueue).addLast(operation)
      if (queue.compareAndSet(oldQueue, newQueue)) {
        if (oldQueue.empty)
          executor.execute(this)
        return
      }
    }
  }

  private void doOperation(Operation<T> operation) {
    pendingCalls.set(FList.emptyList)

    Throwable error
    try {
      def oldValue = value
      def newValue = operation(oldValue)
      validate(newValue)
      value = newValue
      notifyListeners(oldValue, newValue)
    } catch (e) {
      error = e
    }

    def pending = pendingCalls.get()
    pendingCalls.set(null)

    if (!error) {
      def andContinue = operation.andContinue
      if(andContinue) {
        int added = 0
        AtomicInteger nestedSendCounter
        for (pc in pending.reverse()) {
          def oldContinuation = pc.operation.andContinue
          if(oldContinuation) {
              if(nestedSendCounter == null)
                nestedSendCounter = []

              nestedSendCounter.addAndGet(2)
              added++
              pc.operation.andContinue = {
                try {
                  oldContinuation.run()
                }
                finally {
                  if(!nestedSendCounter.decrementAndGet()) {
                    andContinue.run ()
                  }
                }
              }
          }

          pc.agent.call(pc.operation, pc.operation.andContinue)
        }

        if(!added || nestedSendCounter.addAndGet(-added) == 0) {
            nestedSendCounter = null
        }

        if(nestedSendCounter == null)
          andContinue.run ()
      }
      else {
        // no continuation
        for (pc in pending.reverse()) {
          pc.agent.call(pc.operation)
        }
      }
    }
    else {
      try {
        onError(error)
      }
      catch (Throwable t) { // ignore
      }
    }
  }

  protected void setError(Throwable error) {
    value = new ErrorValue(error:error)
  }

  protected void onError(Throwable error) {
    setError(error)
  }

  void run() {
    for (;;) {
      def q = queue
      def removed = q.removeFirst()
      if (q.size() == 1) {
        if (queue.compareAndSet(q, busyEmptyQueue)) {
          doOperation removed.first
          if (!queue.compareAndSet(busyEmptyQueue, FQueue.emptyQueue)) {
            executor.execute this
          }
          return
        }
      }
      else {
        if (queue.compareAndSet(q, removed.second)) {
          doOperation removed.first
          executor.execute this
          return
        }
      }
    }
  }
}
