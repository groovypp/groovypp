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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

@GrUnit({
    testWithFixedPool(10) {
        ResourcePool<String> rpool = [
            executor: pool,
            initResources: { ["a"] }
        ]

        rpool.execute { it.toUpperCase() } {
            assert it == 'A'
        }

        rpool.execute {
            rpool.add("b")
            rpool.execute {
                assert it == 'b'
            }.get()
        }
    }
})
@Typed abstract class ResourcePool<R> {
    Executor executor
    boolean  runFair

    private volatile Pair<FQueue<Function1<R,Object>>,FList<R>> state = [FQueue.emptyQueue,null]

    /**
     * @return created pooled resources
     */
    abstract Iterable<R> initResources ()

    abstract static class Action<R,D> extends BindLater<D> implements Function1<R,D> {}

    abstract static class Allocate<R> implements Function1<R,Object> {}

    final <D> BindLater<D> execute (Action<R,D> action, BindLater.Listener<D> whenDone = null) {
        action.whenBound(whenDone)

        if (state.second == null) {
            initPool ()
        }
        for (;;) {
            def s = state
            if (s.second.empty) {
                // no resource available, so put action in to the queue
                if(state.compareAndSet(s, [s.first.addLast(action), FList.emptyList]))
                    return action
            }
            else {
                // queue is guaranteed to be empty
                if(state.compareAndSet(s, [FQueue.emptyQueue, s.second.tail])) {
                    if(!isResourceAlive(s.second.head))
                      continue

                    // schedule action
                    executor.execute {
                        scheduledAction(action,s.second.head)
                    }
                    return action
                }
            }
        }
    }

    final void allocateResource (Allocate<R> action) {
        if (state.second == null) {
            initPool ()
        }
        for (;;) {
            def s = state
            if (s.second.empty) {
                // no resource available, so put action in to the queue
                if(state.compareAndSet(s, [s.first.addLast(action), FList.emptyList]))
                    return
            }
            else {
                // queue is guaranteed to be empty
                if(state.compareAndSet(s, [FQueue.emptyQueue, s.second.tail])) {
                    if(!isResourceAlive(s.second.head))
                      continue

                    // schedule action
                    executor.execute {
                        scheduledAction(action,s.second.head)
                    }
                    return
                }
            }
        }
    }

    void releaseResource (R resource) {
        if(!isResourceAlive(resource))
          return

        for (;;) {
            def s = state
            if (s.first.empty) {
                // no more actions => we return resource to the pool
                if(state.compareAndSet(s, [FQueue.emptyQueue, s.second + resource])) {
                    break
                }
            }
            else {
                def removed = s.first.removeFirst()
                if(state.compareAndSet(s, [removed.second, s.second])) {
                    executor.execute {
                        scheduledAction(removed.first,resource)
                    }
                    return
                }
            }
        }
    }

    private final <D> Object scheduledAction(Function1<R,D> action, R resource) {
        switch(action) {
            case Action:
              try {
                  action.set(action(resource))
              }
              catch(t) {
                  action.setException(t)
              }
            break

            case Allocate:
              action(resource)
              return
            break
        }

        if(!isResourceAlive(resource))
          return

        for (;;) {
            def s = state
            if (s.first.empty) {
                // no more actions => we return resource to the pool
                if(state.compareAndSet(s, [FQueue.emptyQueue, s.second + resource])) {
                    break
                }
            }
            else {
                def removed = s.first.removeFirst()
                if(state.compareAndSet(s, [removed.second, s.second])) {
                    if (runFair) {
                        // schedule action
                        executor.execute {
                            scheduledAction(removed.first,resource)
                        }

                        break
                    }
                    else {
                        // tail recursion
                        return scheduledAction(removed.first, resource)
                    }
                }
            }
        }
    }

    void add(R resource) {
        if (state.second == null) {
            initPool ()
        }

        if(!isResourceAlive(resource))
          return

        for(;;) {
            def s = state
            if (!s.second.empty) {
                // we guaranteed that there is no waiting tasks
                if(state.compareAndSet(s, [FQueue.emptyQueue, s.second + resource])) {
                    break
                }
            }
            else {
                if(state.first.empty) {
                    // no pending tasks
                    if(state.compareAndSet(s, [FQueue.emptyQueue, s.second + resource])) {
                        break
                    }
                }
                else {
                    def removed = s.first.removeFirst()
                    if(state.compareAndSet(s, [removed.second, FList.emptyList])) {
                        scheduledAction(removed.first, resource)
                        break
                    }
                }
            }
        }
    }

    boolean isResourceAlive(R resource) {
        true
    }

    public synchronized void initPool () {
        if (state.second == null) {
            state.second = FList.emptyList.addAll(initResources ())
        }
    }
}
