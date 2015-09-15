package com.github.bluzwang.aopcache.cache;

import java.util.LinkedHashMap;

/**
 * Created by wangzhijie@wind-mobi.com on 2015/9/15.
 */
public class LruMap<K,V> extends LinkedHashMap<K,V> {
    /** serialVersionUID */
    private static final long serialVersionUID = -5933045562735378538L;
    /** 最大数据存储容量 */
    private static final int  LRU_MAX_CAPACITY     = 1024;
    /** 存储数据容量  */
    private int               capacity = LRU_MAX_CAPACITY;

    public LruMap() {
        super();
    }

    /**
     * 带参数构造方法
     * @param initialCapacity   容量
     * @param loadFactor        装载因子
     * @param isLRU             是否使用lru算法，true：使用（按方案顺序排序）;false：不使用（按存储顺序排序）
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
