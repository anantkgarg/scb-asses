package com.scb.assessment.cache;

import org.junit.Test;

import java.util.function.Function;

import static org.mockito.Mockito.*;

public class GenericCacheImplTest {

    @Test(expected = NullPointerException.class)
    public void testGetNPEWhenFunctionIsNull() {
        Cache<String, String> cache = new GenericCacheImpl<>(null, 2);
        cache.get("");
    }

    @Test(expected = NullPointerException.class)
    public void testGetNPEWhenCacheIsNull() {
        Cache<String, String> cache = new GenericCacheImpl<>(String::toString, null);
        cache.get("");
    }

    @Test(expected = NullPointerException.class)
    public void testGetNPEWhenKeyIsNull() {
        Cache<String, String> cache = new GenericCacheImpl<>(String::toString, 2);
        cache.get(null);
    }

    @Test(expected = NullPointerException.class)
    public void testGetNPEWhenValueIsNull() {
        Function<String, String> mock = mock(Function.class);
        when(mock.apply(any(String.class))).thenReturn(any(String.class));
        Cache<String, String> cache = new GenericCacheImpl<>(mock, 2);
        cache.get("test");
    }

    @Test
    public void testGet() {
        Function<String, String> mock = mock(Function.class);
        when(mock.apply(any(String.class))).thenReturn("TEST");
        Cache<String, String> cache = new GenericCacheImpl<>(mock, 2);

        cache.get("test");
        cache.get("test1");
        cache.get("test");

        verify(mock, times(1)).apply("test");
        verify(mock, times(2)).apply(anyString());
    }
}