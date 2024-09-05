/*
 * Copyright (C) 2015 Baidu, Inc. All Rights Reserved.
 */

package com.baidu.mapapi.clusterutil.clustering.view;

import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;

import com.baidu.mapapi.clusterutil.clustering.Cluster;
import com.baidu.mapapi.clusterutil.clustering.ClusterItem;
import com.baidu.mapapi.clusterutil.clustering.ClusterManager;
import com.baidu.mapapi.map.Marker;
import java.util.Set;

/** 负责处理聚合操作（聚合点渲染器） Renders clusters.
 * @author aking*/
public interface ClusterRenderer<T extends ClusterItem> {

  /**
   * Called when the view needs to be updated because new clusters need to be displayed.
   * 当有新的聚合点时 调用用
   * @param clusters the clusters to be displayed.
   */
  void onClustersChanged(Set<? extends Cluster<T>> clusters);

  void setOnClusterClickListener(ClusterManager.OnClusterClickListener<T> listener);

  void setOnClusterInfoWindowClickListener(
      ClusterManager.OnClusterInfoWindowClickListener<T> listener);

  void setOnClusterItemClickListener(ClusterManager.OnClusterItemClickListener<T> listener);

  void setOnClusterItemInfoWindowClickListener(ClusterManager.OnClusterItemInfoWindowClickListener<T> listener);

  /**
   * 通过marker 返回聚合点数据(单个)
   *
   * @param marker
   * @return
   */
  @Nullable
  T findClusterMarkerData(Marker marker);

  @Nullable
  Set<T> findClusterSingleMarkerDatas();

  @Nullable
  Set<Cluster<T>> findClusterMarkerDatas();

  /**
   * 返回聚合点数据
   *
   * @param marker
   * @return
   */
  @Nullable
  Cluster<T> findClusterMarkersData(Marker marker);

  /** Called when the view is added. */
  void onAdd();

  /** Called when the view is removed. */
  void onRemove();

  /** Called to set animation on or off */
  void setAnimation(boolean animate);

  /**
   * Sets the length of the animation in milliseconds.
   */
  void setAnimationDuration(long animationDurationMs);

  /**
   * Called to determine the color of a Cluster.
   */
  int getColor(int clusterSize);

  /**
   * Called to determine the text appearance of a cluster.
   */
  @StyleRes
  int getClusterTextAppearance(int clusterSize);
}
