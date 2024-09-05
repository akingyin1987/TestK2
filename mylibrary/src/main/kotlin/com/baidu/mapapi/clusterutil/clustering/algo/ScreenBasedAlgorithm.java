/*
 * Copyright (c) 2020. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 * akingyin@163.com
 */

package com.baidu.mapapi.clusterutil.clustering.algo;

import com.baidu.mapapi.clusterutil.clustering.ClusterItem;
import com.baidu.mapapi.map.MapStatus;

/**
 * 基于屏幕的算法 This algorithm uses map position for clustering, and should be reclustered on map
 * movement
 *
 * @author king
 * @version V1.0 @ Description: @ Date 2020/6/23 10:39
 */
public interface ScreenBasedAlgorithm<T extends ClusterItem> extends Algorithm<T> {

  /**
   * 是否允许在移动是重新调整聚合算法
   *
   * @return
   */
  boolean shouldReclusterOnMapMovement();

  /**
   * 百度地图状态发生改变
   *
   * @param mapStatus
   */
  void onMapStatusChange(MapStatus mapStatus);
}
