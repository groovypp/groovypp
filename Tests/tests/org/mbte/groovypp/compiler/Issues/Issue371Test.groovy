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
package org.mbte.groovypp.compiler.Issues

class Issue371Test extends GroovyShellTestCase {
  void testMe () {
    shell.evaluate """
@Typed package p

import groovypp.concurrent.FList

final class FListMap<K,V> implements Iterable<Map.Entry<K,V>> {
  public static final FListMap empty = new FListMap(FList.emptyList)
  private final FList<MyEntry<K, V>> list

  static <K, V> FListMap<K, V> getEmptyMap() {
    empty
  }

  private FListMap(FList<MyEntry<K, V>> list) {
    this.list = list
  }

  FListMap<K, V> put(K key, V value) {
    def existing = get(key)
    if (existing == value) return this
    return [remove(key).list + new MyEntry(key, value)]
  }

  FListMap<K, V> remove(K key) {
    MyEntry<K,V> existing = list.find { it.key == key }
    if (existing) {
      return [list.remove(existing)]
    }
    return this
  }

  FListMap<K, V> plus(Map<K, V> map) {
    Reference ref = [this]
    map.each { k, v -> ref = ref.put(k, v) }
    return ref
  }

  V get(K key) {
    MyEntry<K, V> existing = list.find { it.key == key }
    return existing?.value
  }

  V getAt(K key) {
    return get(key)
  }

  V getUnresolvedProperty(K key) {
    return get(key)
  }

  Iterator<? extends Map.Entry<K, V>> iterator() {
    return list.iterator()
  }

  private static class MyEntry<K, V> implements Map.Entry<K, V> {
    final K key
    final V value

    MyEntry(K key, V value) {
      this.key = key
      this.value = value
    }

    V setValue(V v) {
      throw new UnsupportedOperationException("setValue is not implemented")
    }
  }

}

FListMap.empty.put('mama', 'papa')
    """
  }

  void test2 () {
      shell.evaluate """
    @Typed package p

    class X<K,V> extends HashMap<K,V>{
        X<K, V> plus(Map<K, V> map) {
            Reference ref = [this]
            map.each { k, v ->
                // no need in .set() here but we use cast instead
                // .get() needed as Reference has protected method clone so we have clash
                ref = (X<K,V>)ref.get().clone()
                ref.put(k, v)
            }
            // no need in .get() here because of cast to return type
            ref
        }
    }

    X x = [:]
    x += [mama: 'mia', baba: 'manja']
    assert x['mama'] == 'mia'
    assert x['baba'] == 'manja'
      """
  }
}
