/*
 * Copyright (C) 2015 Baidu, Inc. All Rights Reserved.
 */

package com.baidu.mapapi.clusterutil.clustering.algo;

import androidx.collection.LruCache;
import com.baidu.mapapi.clusterutil.clustering.Cluster;
import com.baidu.mapapi.clusterutil.clustering.ClusterItem;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 装饰类,主要负责管理缓存的一些操作
 * Optimistically fetch clusters for adjacent zoom levels, caching them as necessary.
 * @author aking
 *
 */
public class PreCachingAlgorithmDecorator<T extends ClusterItem> extends AbstractAlgorithm<T> {
  private final Algorithm<T> mAlgorithm;

  private final LruCache<Integer, Set<? extends Cluster<T>>> mCache = new LruCache<Integer, Set<? extends Cluster<T>>>(5);
  private final ReadWriteLock mCacheLock = new ReentrantReadWriteLock();
  private final Executor mExecutor = Executors.newCachedThreadPool();

  public PreCachingAlgorithmDecorator(Algorithm<T> algorithm) {
    mAlgorithm = algorithm;
  }

  @Override
  public boolean addItem(T item) {
    boolean addItem = mAlgorithm.addItem(item);
    if (addItem) {
      clearCache();
    }
    return addItem;
  }

  @Override
  public boolean addItems(Collection<T> items) {
    boolean addItems = this.mAlgorithm.addItems(items);
    if (addItems) {
      clearCache();
    }
    return addItems;
  }

  @Override
  public void clearItems() {
    mAlgorithm.clearItems();
    clearCache();
  }

  @Override
  public boolean removeItem(T item) {
    boolean removeItem = this.mAlgorithm.removeItem(item);
    if (removeItem) {
      clearCache();
    }
    return removeItem;
  }

  @Override
  public boolean removeItems(Collection<T> collection) {
    boolean removeItems = this.mAlgorithm.removeItems(collection);
    if (removeItems) {
      clearCache();
    }
    return removeItems;
  }

  @Override
  public boolean updateItem(T item) {
    boolean updateItem = this.mAlgorithm.updateItem(item);
    if (updateItem) {
      clearCache();
    }
    return updateItem;
  }

  private void clearCache() {
    mCache.evictAll();
  }

  @Override
  public Set<? extends Cluster<T>> getClusters(double zoom) {

    int discreteZoom = (int) zoom;
    Set<? extends Cluster<T>> results = getClustersInternal(discreteZoom);

    if (mCache.get(discreteZoom + 1) == null) {
      mExecutor.execute(new PrecacheRunnable(discreteZoom + 1));
    }
    if (mCache.get(discreteZoom - 1) == null) {
      mExecutor.execute(new PrecacheRunnable(discreteZoom - 1));
    }
    return results;
  }

  @Override
  public Collection<T> getItems() {
    return mAlgorithm.getItems();
  }

  @Override
  public void setMaxDistanceBetweenClusteredItems(int maxDistance) {
    mAlgorithm.setMaxDistanceBetweenClusteredItems(maxDistance);

    clearCache();
  }

  @Override
  public int getMaxDistanceBetweenClusteredItems() {
    return mAlgorithm.getMaxDistanceBetweenClusteredItems();
  }

  private Set<? extends Cluster<T>> getClustersInternal(int discreteZoom) {

    mCacheLock.readLock().lock();
    Set<? extends Cluster<T>> results = mCache.get(discreteZoom);
    mCacheLock.readLock().unlock();

    if (results == null) {
      mCacheLock.writeLock().lock();
      results = mCache.get(discreteZoom);
      if (results == null) {
        results = mAlgorithm.getClusters(discreteZoom);

        mCache.put(discreteZoom, results);
      }
      mCacheLock.writeLock().unlock();
    }
    return results;
  }

  private class PrecacheRunnable implements Runnable {
    private final int mZoom;

    public PrecacheRunnable(int zoom) {
      mZoom = zoom;
    }

    @Override
    public void run() {
      try {
        // Wait between 500 - 1000 ms.
        Thread.sleep((long) (Math.random() * 500 + 500));
      } catch (InterruptedException e) {
        // ignore. keep going.
      }
      getClustersInternal(mZoom);
    }
  }
}
