package com.scb.assessment.cache;

public interface Cache<K, V> {
    V get(K key);
}
