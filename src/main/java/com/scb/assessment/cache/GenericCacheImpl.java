package com.scb.assessment.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

public class GenericCacheImpl<K, V> implements Cache<K, V> {

    private final Function<K, V> function;
    private final Map<K, V> cache;
    private final ReentrantLock lock = new ReentrantLock();

    public GenericCacheImpl(Function<K, V> function, Map<K, V> cache) {
        this.function = function;
        this.cache = cache;
    }

    public GenericCacheImpl(Function<K, V> function, int cacheSize) {
        this(function, new ConcurrentHashMap<>(cacheSize));
    }

    @Override
    public V get(K key) {
        if(function == null)
            throw new NullPointerException("Function is null");

        if(cache == null)
            throw new NullPointerException("Cache is null");

        if (key == null)
            throw new NullPointerException("Key cannot be null");

        V value;
        if((value = cache.get(key)) == null) {
            value = applyAndAdd(key);
        }
        return value;
    }

    private V applyAndAdd(K key) {
        V value;
        try {
            lock.lock();
            if((value = cache.get(key)) == null) {
                value = function.apply(key);
                if(value == null)
                    throw new NullPointerException("Derived value is null");
                cache.put(key, value);
            }
        }
        finally {
            lock.unlock();
        }
        return value;
    }
}
