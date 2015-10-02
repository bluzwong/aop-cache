package com.github.bluzwang.aopcache.cache;

import java.util.LinkedHashMap;

/**
 * Created by wangzhijie@wind-mobi.com on 2015/9/15.
 */
public class LruMap<K,V> extends LinkedHashMap<K,V> {
    private static final long serialVersionUID = -5933045562735378538L;
    private static final int  LRU_MAX_CAPACITY     = 1024;
    private int               capacity = LRU_MAX_CAPACITY;

    public LruMap() {
        super();
    }

    /**
     */
    public LruMap(int initialCapacity, float loadFactor, boolean isLRU) {
        super(initialCapacity, loadFactor, true);
        capacity = LRU_MAX_CAPACITY;
    }
    public LruMap(int initialCapacity, float loadFactor, boolean isLRU, int lruCapacity) {
        super(initialCapacity, loadFactor, true);
        this.capacity = lruCapacity;
    }
    @Override
    protected boolean removeEldestEntry(Entry<K, V> eldest) {
        return size() > capacity;
    }
}
