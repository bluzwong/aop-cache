package com.github.bluzwang.aopcache.cache;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;


/**
 * Created by wangzhijie@wind-mobi.com on 2015/9/15.
 */
public class CacheInfo extends RealmObject {

    @PrimaryKey
    private String key;

    private String objGuid;

    private long editTime;

    private long expTime;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getObjGuid() {
        return objGuid;
    }

    public void setObjGuid(String objGuid) {
        this.objGuid = objGuid;
    }

    public long getEditTime() {
        return editTime;
    }

    public void setEditTime(long editTime) {
        this.editTime = editTime;
    }

    public long getExpTime() {
        return expTime;
    }

    public void setExpTime(long expTime) {
        this.expTime = expTime;
    }
}
