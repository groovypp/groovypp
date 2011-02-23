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

/**
 * Atomic reference, which also supports validation and listeners
 *
 * @param < T > type of referenced data
 */
@Typed class Atom<T> extends AbstractRef<T> {
  private volatile T value

  Atom (T value = null) {
    this.value = value
  }

  final T get() { value }

  final T call(Function1<T,T> mutation) {
    for(;;) {
      def oldValue = value
      def newValue = mutation(oldValue)
      validate(newValue)
      if(value.compareAndSet(oldValue, newValue)) {
        notifyListeners(oldValue, newValue)
        return newValue
      }
    }
  }

  final boolean compareAndSet(Object oldValue, Object newValue){
    validate(newValue)
    if(value.compareAndSet(oldValue, newValue)) {
      notifyListeners(oldValue, newValue)
      return true
    }
  }

  final void set(T newValue){
      def oldValue = value
      validate(newValue)
      value = newValue
      notifyListeners(oldValue, newValue)
  }

  final T getAndSet(T newValue) {
      for (;;) {
          def oldValue = value
          validate(newValue)
          if (value.compareAndSet(oldValue, newValue)) {
            notifyListeners(oldValue, newValue)
            return oldValue
          }
      }
  }
}
