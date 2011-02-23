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
package groovypp.concurrent

import java.util.concurrent.Executor

/**
 * Base class for all types of concurrent references
 *
 * All references supports validation and listeners
 *
 * @param < T > - type of referenced data
 */
@Typed abstract class AbstractRef<T> {

  /**
   * Listener for changes of referenced data
   *
   * @param < T > - type of referenced data
   */
  abstract static class Listener<T> {
    abstract void call(T oldValue, T newValue)
  }

  private volatile FList<Listener<T>> listeners = FList.emptyList

  /**
   * @return currently referenced value
   */
  abstract T get()

  /**
   * Add listener for changes of referenced data
   *
   * @param listener listener to add
   */
  final Listener<T> addListener(Listener<T> listener, Executor executor = null) {
    if(executor) {
      def myListener = listener
      listener = { oldValue, newValue ->
        executor.execute {
          myListener(oldValue, newValue)
        }
      }
    }

    for(;;) {
      def old = listeners
      if(listeners.compareAndSet(old, old + listener))
        break
    }

    listener
  }

  /**
   * Remove listener for changes of referenced data
   *
   * @param listener listener to remove
   */
  final void removeListener(Listener<T> listener) {
    for(;;) {
      def old = listeners
      if(listeners.compareAndSet(old, old - listener))
        break
    }
  }

  protected final notifyListeners (FList<Listener<T>> listeners = this.listeners, T oldValue, T newValue) {
    for(listener in listeners) {
      listener(oldValue, newValue)
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
  protected boolean doValidate(T value) { true }

  protected final void validate(T value) {
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
}