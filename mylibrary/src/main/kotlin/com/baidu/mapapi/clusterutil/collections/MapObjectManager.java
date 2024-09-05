/*
 * Copyright (c) 2020. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 * akingyin@163.com
 */

package com.baidu.mapapi.clusterutil.collections;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.baidu.mapapi.map.BaiduMap;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;


/**
 * 抽象基础的Map对象集合管理器类的实现。它主要用于追踪地图上的对象集合，并将所有与对象相关的事件委派给各自管理的单独监听器
 * Abstract base implementation for map object collection manager classes.
 * <p/>
 * Keeps track of collections of objects on the map. Delegates all object-related events to each
 * collection's individually managed listeners.
 * <p/>
 * All object operations (adds and removes) should occur via its collection class. That is, don't
 * add an object via a collection, then remove it via Object.remove()
 */
abstract class MapObjectManager<O, C extends MapObjectManager.Collection> {
  protected final BaiduMap mMap;

  private final Map<String, C> mNamedCollections = new HashMap<>();
  protected final Map<O, C> mAllObjects = new HashMap<>();

  public MapObjectManager(@NonNull BaiduMap map) {
    mMap = map;
    new Handler(Looper.getMainLooper()).post(new Runnable() {
      @Override
      public void run() {
        setListenersOnUiThread();
      }
    });
  }

  abstract void setListenersOnUiThread();

  public abstract C newCollection();

  /**
   * Create a new named collection, which can later be looked up by {@link #getCollection(String)}
   *
   * @param id a unique id for this collection.
   */
  public C newCollection(String id) {
    if (mNamedCollections.get(id) != null) {
      throw new IllegalArgumentException("collection id is not unique: " + id);
    }
    C collection = newCollection();
    mNamedCollections.put(id, collection);
    return collection;
  }

  /**
   * Gets a named collection that was created by {@link #newCollection(String)}
   *
   * @param id the unique id for this collection.
   */
  public C getCollection(String id) {
    return mNamedCollections.get(id);
  }

  /**
   * Removes an object from its collection.
   *
   * @param object the object to remove.
   * @return true if the object was removed.
   */
  public boolean remove(O object) {
    C collection = mAllObjects.get(object);
    return collection != null && collection.remove(object);
  }

  protected abstract void removeObjectFromMap(O object);

  public class Collection {
    private final Set<O> mObjects = new LinkedHashSet<>();

    public Collection() {
    }

    protected void add(O object) {
      mObjects.add(object);
      mAllObjects.put(object, (C) this);
    }

    protected boolean remove(O object) {
      if (mObjects.remove(object)) {
        mAllObjects.remove(object);
        removeObjectFromMap(object);
        return true;
      }
      return false;
    }

    public void clear() {
      for (O object : mObjects) {
        removeObjectFromMap(object);
        mAllObjects.remove(object);
      }
      mObjects.clear();
    }

    protected java.util.Collection<O> getObjects() {
      //返回一个不可修改的集合
      return Collections.unmodifiableCollection(mObjects);
    }
  }
}
