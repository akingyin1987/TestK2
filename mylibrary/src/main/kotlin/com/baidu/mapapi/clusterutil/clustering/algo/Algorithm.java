/*
 * Copyright (C) 2015 Baidu, Inc. All Rights Reserved.
 */

package com.baidu.mapapi.clusterutil.clustering.algo;

import com.baidu.mapapi.clusterutil.clustering.Cluster;
import com.baidu.mapapi.clusterutil.clustering.ClusterItem;
import java.util.Collection;
import java.util.Set;

/**
 * 算法核心接口
 * Algorithm Logic for computing clusters
 *
 * @author aking
 */
public interface Algorithm<T extends ClusterItem> {

  boolean addItem(T item);

  boolean addItems(Collection<T> collection);

  void clearItems();

  Set<? extends Cluster<T>> getClusters(double zoom);

  Collection<T> getItems();

  int getMaxDistanceBetweenClusteredItems();

  void lock();

  boolean removeItem(T item);

  boolean removeItems(Collection<T> collection);

  void setMaxDistanceBetweenClusteredItems(int maxDistance);

  void unlock();

  boolean updateItem(T item);
}
