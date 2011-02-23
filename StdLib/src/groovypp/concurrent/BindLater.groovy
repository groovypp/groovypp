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

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.AbstractQueuedSynchronizer
import java.util.concurrent.*

/**
 */
@Typed class BindLater<V> extends AbstractQueuedSynchronizer implements Future<V> {
    // any of this bits mean that calculation either completed or (with S_RUNNING) about to complete
    protected static final int S_SET           = 1
    protected static final int S_EXCEPTION     = 2
    protected static final int S_CANCELLED     = 4

    protected static final int S_RUNNING       = 8

    protected static final int S_DONE = S_SET|S_EXCEPTION|S_CANCELLED

    // contains either null or running thread or result or exception
    private volatile def internalData

    private volatile FList boundListeners = FList.emptyList

    final boolean isRunning() {
        getState() & S_RUNNING
    }

    final boolean isCancelled() {
        def s = getState()
        (s & S_CANCELLED) && !(s & S_RUNNING)
    }

    final boolean isException() {
        def s = getState()
        (s & S_EXCEPTION) && !(s & S_RUNNING)
    }

    final boolean isSet() {
        def s = getState()
        (s & S_SET) && !(s & S_RUNNING)
    }

    final boolean isDone() {
        def s = getState()
        (s & S_DONE) && !(s & S_RUNNING)
    }

    final boolean cancel(boolean mayInterruptIfRunning) {
        for (;;) {
            def s = getState()
            if (s & S_DONE)
                return false
            if (compareAndSetState(s, S_CANCELLED|S_RUNNING)) {
                if (mayInterruptIfRunning) {
                    Thread r = internalData
                    if (r)
                        r.interrupt()
                }
                releaseShared(S_CANCELLED)
                done()
                return true
            }
        }
    }

    V get() throws InterruptedException, ExecutionException {
        acquireSharedInterruptibly(0)
        def s = getState()
        if (s == S_CANCELLED)
            throw new CancellationException()
        if (s == S_EXCEPTION) {
            def throwable = (Throwable) internalData
            throw new ExecutionException(throwable.message, throwable)
        }
        (V)internalData
    }

    final V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (!tryAcquireSharedNanos(0, unit.toNanos(timeout)))
            throw new TimeoutException()
        def s = getState()
        if (s == S_CANCELLED)
            throw new CancellationException()
        if (s == S_EXCEPTION) {
            def throwable = (Throwable) internalData
            throw new ExecutionException(throwable.message, throwable)
        }
        (V)internalData
    }

    final BindLater<V> whenBound (Executor executor = null, Listener<V> listener) {
        if (!listener)
            return this

        if(executor) {
          def original = listener
          listener = { data ->
            executor.execute {
              original.onBound data
            }
          }
        }
        
        for (;;) {
            def l = boundListeners
            if (l == null) {
                // it means done() worked already
                listener.onBound(this)
                return this
            }

            if(boundListeners.compareAndSet(l,l + listener))
                return this
        }
    }

    protected void done() {
        for (;;) {
            def l = boundListeners
            if (boundListeners.compareAndSet(l, null)) {
                for (el in l.reverse()) {
                  switch(el) {
                    case BlockingQueue:
                      el.put(this)
                    break

                    case Listener:
                        el.onBound(this)
                    break
                  }
                }
                return
            }
        }
    }

    /**
     * Validates new value before it set
     *
     * IllegalStateException will be thrown if this method return false or thrown exception other than RuntimeException
     *
     * Default implementation of this method always returns true and should be overriden by subclasses
     *
     * @param value data to validate
     * @return
     */
    protected boolean doValidate(V value) { true }

    protected final void validate(V value) {
      try {
        if(!doValidate(value))
          throw new IllegalStateException("Illegal reference state")
      }
      catch(RuntimeException re) {
        throw re
      }
      catch(Throwable t) {
        throw new IllegalStateException("Illegal reference state", t)
      }
    }

    final boolean set(V v) {
        for (;;) {
            def s = getState()
            if (s & S_DONE)
                return false

            try {
              validate(v)
            }
            catch(e) {
              return setException(e)
            }

            if (compareAndSetState(s, S_SET|S_RUNNING)) {
                internalData = v
                releaseShared(S_SET)
                done()
                return true
            }
        }
    }

    final boolean setException(Throwable t) {
        for (;;) {
            def s = getState()
            if (s & S_DONE)
                return false
            if (compareAndSetState(s, S_EXCEPTION|S_RUNNING)) {
                def internal = internalData
                internalData = t
                releaseShared(S_EXCEPTION)
                done()
                return true
            }
        }
    }

    protected final int tryAcquireShared(int ignore) {
        isDone() ? 1 : -1
    }

    protected final boolean tryReleaseShared(int finalState) {
        setState(finalState)
        true
    }

    protected final boolean setRunningThread () {
        def t = Thread.currentThread ()
        for (;;) {
            def s = getState ()

            if (s) {
                return false
            }

            if (compareAndSetState(0, S_RUNNING)) {
                internalData = t
                return true
            }
        }
    }

    abstract static interface Listener<V> {
        abstract void onBound (BindLater<V> data)
    }

    static class Group<V> extends BindLater<V> {
        AtomicInteger counter

        Group (int concurrency) {
            counter = [concurrency]
        }

        void attach (BindLater<V> inner) {
            inner.whenBound { bl ->
                if (!isDone()) {
                    if (bl.exception) {
                        try {
                            bl.get ()
                        }
                        catch (ExecutionException e) {
                            setException(e.cause)
                        }
                    }
                    else {
                        if (!counter.decrementAndGet())
                            set(null)
                    }
                }
            }
        }
    }
}
