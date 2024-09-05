/*
 * Copyright (C) 2015 Baidu, Inc. All Rights Reserved.
 */

package com.baidu.mapapi.clusterutil.clustering.algo;

import com.baidu.mapapi.clusterutil.clustering.Cluster;
import com.baidu.mapapi.clusterutil.clustering.ClusterItem;
import com.baidu.mapapi.model.LatLng;
import java.util.Collection;
import java.util.LinkedHashSet;

/** A cluster whose center is determined upon creation.
 * @author aking*/
public class StaticCluster<T extends ClusterItem> implements Cluster<T> {
  private final LatLng mCenter;
  private final Collection<T> mItems = new LinkedHashSet<T>();

  public StaticCluster(LatLng center) {
    mCenter = center;
  }

  public boolean add(T t) {
    return mItems.add(t);
  }

  @Override
  public LatLng getPosition() {
    return mCenter;
  }

  public boolean remove(T t) {
    return mItems.remove(t);
  }

  @Override
  public Collection<T> getItems() {
    return mItems;
  }

  @Override
  public int getSize() {
    return mItems.size();
  }

  @Override
  public String toString() {
    return "StaticCluster{" + "mCenter=" + mCenter + ", mItems.size=" + mItems.size() + '}';
  }

  @Override
  public int hashCode() {
    return mCenter.hashCode() + mItems.hashCode();
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof StaticCluster<?>)) {
      return false;
    }

    return ((StaticCluster<?>) other).mCenter.equals(mCenter)
        && ((StaticCluster<?>) other).mItems.equals(mItems);
  }
}
