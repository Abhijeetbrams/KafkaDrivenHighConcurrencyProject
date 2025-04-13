
package com.ecommerce.kafkahighconcurrencyproject.util;

import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


@Service
@Scope("prototype")
public class CacheUtil<K, V> {

    ConcurrentMap<K, V> cacheMap = new ConcurrentHashMap<>();
    CacheValue<K, V> value;

    public void setValue(CacheValue<K, V> value) {
        this.value = value;
    }

    /**
     * Get Value of a Key from Cache
     * 
     * @param key
     * @return
     */
    public V get(K key) {
        return cacheMap.get(key);
    }

    /**
     * Get the cache map
     * 
     * @return
     */
    public ConcurrentMap<K, V> get() {
        return cacheMap;
    }

    /**
     * Blocking sync operation to be used at startup only.
     */
    public void sync() {
        if (value != null)
            cacheMap = value.get();
    }

    /**
     * Async Operation for syncing cache when any record is modified
     */
    @Async
    public void async() {
        sync();
    }

    /**
     * Executed automatically to perform sync behind the scenes
     */
    @Scheduled(fixedDelay = 5 * 60 * 1000, initialDelay = 5 * 60 * 1000)
    public void autoSync() {
        sync();
    }

    public interface CacheValue<K, V> {
        ConcurrentMap<K, V> get();
    }
}