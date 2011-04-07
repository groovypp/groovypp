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

import java.util.Map.Entry
import java.util.concurrent.Callable

/**
 * A clean-room port of Rich Hickey's persistent hashCode trie implementation from
 * Clojure (http://clojure.org).  Originally presented as a mutable structure in
 * a paper by Phil Bagwell.
 *
 * @author Daniel Spiewak
 * @author Rich Hickey
 * @author Alex Tkachman
 */
@Typed abstract class FHashMap<K, V> implements Iterable<Map.Entry<K, V>>, Serializable {
    abstract int size()

    final V getAt(K key) { getAt(0, key, key.hashCode()) }

    final V get(Object key) { getAt(0, key, key.hashCode()) }

    final V get(K key, V defaultValue) { getAt(0, key, key.hashCode()) ?: defaultValue }

    final V getOr(K key, Callable<V> defaultValue) { getAt(0, key, key.hashCode()) ?: defaultValue() }

    final V getUnresolvedProperty(K key) { getAt(0, key, key.hashCode()) }

    final FHashMap<K, V> putAll(Map<K,V> map) {
        def res = this
        for(e in map.entrySet())
            res = res.put(e.key, e.value)
        res
    }

    final FHashMap<K, V> putAll(FHashMap<K,V> map) {
        def res = this
        for(e in map.entrySet())
            res = res.put(e.key, e.value)
        res
    }

    final FHashMap<K, V> plus(FHashMap<K,V> map) {
        putAll(map)
    }

    final FHashMap<K, V> minus(FHashMap<K,V> map) {
        def res = this
        for(e in map.entrySet())
            res = res.remove(e.key)
        res
    }

    final FHashMap<K, V> plus(Map<K,V> map) {
        putAll(map)
    }

    final FHashMap<K, V> minus(Map<K,V> map) {
        def res = this
        for(e in map.entrySet())
            res = res.remove(e.key)
        res
    }

    static <K,V> FHashMap<K,V> create(Map<K,V> map) {
        FHashMap.emptyMap.putAll(map)
    }

    final FHashMap<K, V> put(K key, V value) {
        update(0, key, key.hashCode(), value)
    }

    final FHashMap<K, V> remove(K key) {
        remove(0, key, key.hashCode())
    }

    protected abstract V getAt(int shift, K key, int hash)

    protected abstract FHashMap<K, V> update(int shift, K key, int hash, V value)

    protected abstract FHashMap<K, V> remove(int shift, K key, int hash)

    final AbstractSet<Map.Entry<K, V>> entrySet () { new EntrySet() }
    final AbstractSet<K> keySet () { new KeySet() }
    final Collection<V>  values () { new Values() }

    boolean isEmpty() { !size() }

    boolean containsKey(Object key) { get(key) }

    boolean containsValue(Object value) {
        values().contains(value)
    }

    boolean equals(Object obj) {
        if(obj instanceof FHashMap) {
            if(((FHashMap)obj).size() == size()) {
                for(e in ((FHashMap)obj).entrySet()) {
                    if(this[e.key] != e.value)
                        return false
                }
                return true
            }
        }
    }

    int hashCode() {
        def hash = 0
        for(e in entrySet()) {
            hash += e.key.hashCode()
            hash += e.value.hashCode()
        }
    }

    public static final FHashMap emptyMap = new EmptyNode()

    private static class EmptyNode<K, V> extends FHashMap<K, V> {
        private EmptyNode() {}

        int size() { 0 }

        V getAt(int shift, K key, int hash) { null }

        FHashMap<K, V> update(int shift, K key, int hash, V value) {
            def bits = 1 << ((hash >>> shift) & 0x1f)
            new BitmappedNode(bits, bits, 1, key, value)
        }

        FHashMap<K, V> remove(K key, int hash) { this }

        Iterator<Map.Entry<K, V>> iterator() {
            [
                hasNext: {false},
                next: {throw new NoSuchElementException()},
                remove: {throw new UnsupportedOperationException()}
            ]
        }

        protected FHashMap<K, V> remove(int shift, K key, int hash) {
            this
        }

        protected final Object writeReplace() {
            Serial.instance
        }

        static class Serial implements Serializable {
            static final Serial instance = []

            protected final Object readResolve() {
                FHashMap.emptyMap
            }
        }
    }

    protected static int bitIndex(int bit, int mask) {
        Integer.bitCount(mask & (bit - 1))
    }

    static BitmappedNode bitmap(int shift, int hash, K key, V value, K key2, V value2) {
        int bits1 = 1 << ((hash >>> shift) & 0x1f)
        int bits2 = 1 << ((key2.hashCode() >>> shift) & 0x1f)

        int mask = bits1 | bits2

        int shift1 = bitIndex(bits1, mask) << 1
        int shift2 = bitIndex(bits2, mask) << 1

        if (shift1 == shift2) {
            new BitmappedNode(bits2, bits2, 1, key2, value2).update(shift, key, hash, value)
        } else {
            def newTable = new Object[4]
            newTable[shift1] = key
            newTable[shift1 + 1] = value
            newTable[shift2] = key2
            newTable[shift2 + 1] = value2
            new BitmappedNode(mask, mask, 2, newTable)
        }
    }

    protected static Map.Entry<K, V> mapEntry(int index, Object[] table) {
        if (index < table.length) {
            Entry res = [
                getKey: { table[index] },
                getValue: { table[index + 1] },
                setKey: { throw new UnsupportedOperationException() },
                setValue: { throw new UnsupportedOperationException() },
                toString: {"[$key, $value]" }
            ]
            index += 2
            res
        }
        else {
            throw new NoSuchElementException()
        }
    }

    private static class CollisionNode<K, V> extends FHashMap<K, V> implements Externalizable {
        int hash
        Object[] table

        CollisionNode() {
        }

        CollisionNode(int hash, Object[] table) {
            this.table = table
            this.hash = hash
        }

        int size() {
            table.length >> 1
        }

        V getAt(int shift, K key, int hash) {
            for (int i = 0; i != table.length; i += 2) {
                if (key.equals(table[i]))
                    return table[i + 1]
            }
        }

        public Iterator<Map.Entry<K, V>> iterator() {
            [
                index: 0,
                hasNext: { index < table.length },
                next: {
                    index += 2
                    mapEntry(index - 2, table)
                },
                remove: { throw new UnsupportedOperationException() }
            ]
        }

        FHashMap<K, V> update(int shift, K key, int hash, V value) {
            if (this.hash == hash) {
                for (int i = 0; i != table.length; i += 2) {
                    if (key.equals(table[i])) {
                        if (table[i + 1] === value && table[i] === key)
                            return this
                        else {
                            Object[] newTable = table.clone()
                            newTable[i + 1] = value
                            return new CollisionNode(hash, newTable);
                        }
                    }
                }

                def newTable = new Object[table.length + 2]
                System.arraycopy table, 0, newTable, 2, table.length
                newTable[0] = key
                newTable[1] = value
                new CollisionNode(hash, newTable)
            }
            else {
                int bit = 1 << ((hash >>> shift) & 0x1f)
                FHashMap bitmap = new BitmappedNode(bit, bit, 1, key, value)
                for (int i = 0; i != table.length; i += 2) {
                    bitmap = bitmap.update(shift, table[i], this.hash, table[i + 1])
                }
                bitmap
            }
        }

        FHashMap<K, V> remove(int shift, K key, int hash) {
            for (int i = 0; i != table.length; i += 2) {
                if (key.equals(table[i])) {
                    if (table.length == 4) {
                        // no collision any more
                        if (i == 0) {
                            int bit = 1 << ((table[2].hashCode() >>> shift) & 0x1f)
                            return new BitmappedNode(bit, bit, 1, table[2], table[3])
                        }
                        else {
                            int bit = 1 << ((table[0].hashCode() >>> shift) & 0x1f)
                            return new BitmappedNode(bit, bit, 1, table[0], table[1])
                        }
                    }
                    else {
                        return new CollisionNode(hash, table.remove(i, 2))
                    }
                }
            }

            this
        }

        void writeExternal(ObjectOutput out) {
            out.writeInt hash
            out.writeByte table.length
            for (t in table)
                out.writeObject t
        }

        void readExternal(ObjectInput input) {
            hash = input.readInt()
            int len = input.readByte()
            table = new Object[len]
            for (int i = 0; i != table.length; ++i)
                table[i] = input.readObject()
        }
    }

    private static class BitmappedNode<K, V> extends FHashMap<K, V> implements Externalizable {
        int bits, leafBits, size
        Object[] table // either (key,value) or (node)

        BitmappedNode() {}

        BitmappedNode(int bits, int leafBits, int size, Object[] table) {
            this.bits = bits
            this.leafBits = leafBits
            this.table = table
            this.size = size
        }

        V getAt(int shift, K key, int hash) {
            int i = (hash >>> shift) & 0x1f
            int bit = 1 << i
            if (bits & bit) {
                int index = bitIndex(bit, bits) + bitIndex(bit, leafBits)
                if (bit & leafBits) {
                    // leaf - t[index] - key, t [index+1] - value
                    key.equals(table[index]) ? table[index + 1] : null
                }
                else {
                    // node - t[index]
                    ((FHashMap) table[index]).getAt(shift + 5, key, hash)
                }
            }
        }

        FHashMap<K, V> update(int shift, K key, int hash, V value) {
            int i = (hash >>> shift) & 0x1f
            int bit = 1 << i
            if (bits & bit) {
                i = bitIndex(bit, bits) + bitIndex(bit, leafBits)
                if (leafBits & bit) {
                    // table [i] - key, table [i+1] - value
                    if (table[i].equals(key)) {
                        if (table[i + 1] === value)
                            return this
                        else {
                            replaceLeafValue(value, i)
                        }
                    }
                    else {
                        leafToNode(i, hash, key, value, shift, bit)
                    }
                }
                else {
                    // table [i] is node
                    def node = ((FHashMap) table[i]).update(shift + 5, key, hash, value)
                    if (node === table[i])
                        return this
                    else {
                        Object[] newTable = table.clone()
                        newTable[i] = node
                        return new BitmappedNode(bits, leafBits, size - ((FHashMap) table[i]).size() + node.size(), newTable)
                    }
                }
            } else {
                insertLeaf(bit, key, value)
            }
        }

        private def insertLeaf(int bit, K key, V value) {
            int newBits = bits | bit
            int newLeafBits = leafBits | bit
            int i = bitIndex(bit, newBits) + bitIndex(bit, newLeafBits)

            def newTable = new Object[table.length + 2]
            if (i < table.length) {
                if (i > 0)
                    System.arraycopy table, 0, newTable, 0, i
                if (i < table.length)
                    System.arraycopy table, i, newTable, i + 2, table.length - i
            }
            else {
                System.arraycopy table, 0, newTable, 0, table.length
            }

            newTable[i] = key
            newTable[i + 1] = value

            return new BitmappedNode(newBits, newLeafBits, size + 1, newTable)
        }

        private FHashMap leafToNode(int i, int hash, K key, V value, int shift, int bit) {
            if (hash == table[i].hashCode()) {
                def collisionNode = new CollisionNode(hash, table[i], table[i + 1], key, value)

                if (table.length == 2)
                    collisionNode
                else {
                    def newTable = table.remove(i + 1)
                    newTable[i] = collisionNode
                    return new BitmappedNode(bits, leafBits & (~bit), size + 1, newTable)
                }
            }
            else {
                def newTable = table.remove(i + 1)
                newTable[i] = bitmap(shift + 5, hash, key, value, table[i], table[i + 1])
                return new BitmappedNode(bits, leafBits & (~bit), size + 1, newTable)
            }
        }

        private FHashMap replaceLeafValue(V value, int i) {
            Object[] newTable = table.clone()
            newTable[i + 1] = value
            new BitmappedNode(bits, leafBits, size, newTable)
        }

        FHashMap<K, V> remove(int shift, K key, int hash) {
            int i = (hash >>> shift) & 0x1f
            int bit = 1 << i
            if (bits & bit) {
                i = bitIndex(bit, bits) + bitIndex(bit, leafBits)
                if (leafBits & bit) {
                    if (table[i].equals(key)) {
                        def newBits = bits & ~bit
                        if (newBits) {
                            new BitmappedNode(newBits, leafBits & (~bit), size - 1, table.remove(i, 2))
                        }
                        else {
                            // it was only one
                            emptyMap
                        }
                    }
                    else {
                        this
                    }
                }
                else {
                    def node = ((FHashMap) table[i]).remove(shift + 5, key, hash)
                    if (node === table[i]) {
                        return this
                    } else if (node === emptyMap) {
                        int adjustedBits = bits & ~bit
                        if (!adjustedBits)
                            return emptyMap
                        return new BitmappedNode(adjustedBits, leafBits, size - 1, table.remove(i))
                    } else {
                        Object[] newTable = table.clone()
                        newTable[i] = node
                        return new BitmappedNode(bits, leafBits, size - ((FHashMap) table[i]).size() + node.size(), newTable)
                    }
                }
            }
            else {
                this
            }
        }

        Iterator<Map.Entry<K, V>> iterator() {
            [
                    curBit: 0,
                    curIterator: (Iterator<Map.Entry<K, V>>) null,
                    hasNext: {
                        if (curIterator != null) {
                            if (curIterator.hasNext())
                                return true
                            curIterator = null
                            curBit++
                        }

                        for(; curBit != 32; curBit++) {
                            int bit = 1 << curBit
                            if(bits & bit) {
                                if (leafBits & bit) {
                                    return true
                                }
                                else {
                                    int index = bitIndex(bit, bits) + bitIndex(bit, leafBits)
                                    curIterator = ((FHashMap<K, V>) table[index]).iterator()
                                    return curIterator.hasNext()
                                }
                            }
                        }
                    },
                    next: {
                        if (curIterator != null) {
                            return curIterator.next()
                        }

                        def bit = 1 << curBit
                        if (leafBits & bit) {
                            curBit++
                            return mapEntry(bitIndex(bit, bits) + bitIndex(bit, leafBits), table)
                        }
                        else {
                            throw new NoSuchElementException()
                        }
                    },
                    remove: { throw new UnsupportedOperationException() }
            ]
        }

        void writeExternal(ObjectOutput out) {
            out.writeInt bits
            out.writeInt leafBits
            out.writeInt size
            out.writeByte table.length
            for (t in table)
                out.writeObject t
        }

        void readExternal(ObjectInput input) {
            bits = input.readInt()
            leafBits = input.readInt()
            size = input.readInt()
            int len = input.readByte()
            table = new Object[len]
            for (int i = 0; i != table.length; ++i)
                table[i] = input.readObject()
        }

        @Override
        int size() {
            return size
        }
    }

    static <T> T[] remove(T[] array, int index) {
        def newArray = new Object[array.length - 1]
        if (index > 0)
            System.arraycopy array, 0, newArray, 0, index
        if (index < array.length - 1)
            System.arraycopy array, index + 1, newArray, index, array.length - 1 - index
        newArray
    }

    static <T> T[] remove(T[] array, int index, int count) {
        def newArray = new Object[array.length - count]
        if (index > 0)
            System.arraycopy array, 0, newArray, 0, index
        if (index < array.length - 1)
            System.arraycopy array, index + count, newArray, index, array.length - count - index
        newArray
    }

    private final class EntrySet<K,V> extends AbstractSet<Map.Entry<K, V>> {
        Iterator<Map.Entry<K, V>> iterator() {
            FHashMap.this.iterator()
        }

        int size() {
            FHashMap.this.size();
        }

        public boolean contains(Object o) {
            o instanceof MapEntry && (FHashMap.this.get(((MapEntry) o).key) != null)
        }

        boolean remove(Object o) {
            throw new UnsupportedOperationException()
        }

        public void clear() {
            throw new UnsupportedOperationException()
        }
    }

    private final class KeySet<K> extends AbstractSet<K> {
        @Typed Iterator<K> iterator() {
            FHashMap.this.iterator()*.key
        }

        int size() {
            FHashMap.this.size();
        }

        public boolean contains(Object o) {
            return FHashMap.this.get(o) != null
        }

        boolean remove(Object o) {
            throw new UnsupportedOperationException()
        }

        public void clear() {
            throw new UnsupportedOperationException()
        }
    }

    private final class Values<V> extends AbstractCollection<V> {
        public Iterator<V> iterator() {
            FHashMap.this.iterator()*.value
        }

        int size() {
            FHashMap.this.size();
        }

        public void clear() {
            throw new UnsupportedOperationException()
        }
    }
}
