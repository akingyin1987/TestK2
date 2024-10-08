/*
 * Copyright (C) 2015 Baidu, Inc. All Rights Reserved.
 */

package com.baidu.mapapi.clusterutil.clustering.algo;

import com.baidu.mapapi.clusterutil.clustering.Cluster;
import com.baidu.mapapi.clusterutil.clustering.ClusterItem;
import com.baidu.mapapi.clusterutil.projection.Bounds;
import com.baidu.mapapi.clusterutil.projection.Point;
import com.baidu.mapapi.clusterutil.projection.SphericalMercatorProjection;
import com.baidu.mapapi.clusterutil.quadtree.PointQuadTree;
import com.baidu.mapapi.model.LatLng;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 真正的算法核心类
 * A simple clustering algorithm with O(nlog n) performance. Resulting clusters are not
 * hierarchical.
 *
 * <p>High level algorithm:<br>
 * 1. Iterate over items in the order they were added (candidate clusters).<br>
 * 2. Create a cluster with the center of the item. <br>
 * 3. Add all items that are within a certain distance to the cluster. <br>
 * 4. Move any items out of an existing cluster if they are closer to another cluster. <br>
 * 5. Remove those items from the list of candidate clusters.
 *
 * <p>Clusters have the center of the first element (not the centroid of the items within it).
 *
 * @author zlcd
 */
public class NonHierarchicalDistanceBasedAlgorithm<T extends ClusterItem>
    extends AbstractAlgorithm<T> {

  /** // essentially 100 dp. */
  private static final int DEFAULT_MAX_DISTANCE_AT_ZOOM = 100;

  private int mMaxDistance = DEFAULT_MAX_DISTANCE_AT_ZOOM;

  /** Any modifications should be synchronized on mQuadTree. */
  private final Collection<QuadItem<T>> mItems = new HashSet<>();

  /** Any modifications should be synchronized on mQuadTree. */
  private final PointQuadTree<QuadItem<T>> mQuadTree =
      new PointQuadTree<NonHierarchicalDistanceBasedAlgorithm.QuadItem<T>>(0, 1, 0, 1);

  private static final SphericalMercatorProjection PROJECTION =
      new SphericalMercatorProjection(1.0d);

  @Override
  public boolean addItem(T item) {
    boolean add;
    QuadItem<T> quadItem = new QuadItem<>(item);
    synchronized (this.mQuadTree) {
      add = this.mItems.add(quadItem);
      if (add) {
        this.mQuadTree.add(quadItem);
      }
    }
    return add;
  }

  @Override
  public boolean addItems(Collection<T> items) {
    boolean z = false;
    for (T t : items) {
      if (addItem(t)) {
        z = true;
      }
    }
    return z;
  }

  @Override
  public void clearItems() {
    synchronized (mQuadTree) {
      mItems.clear();
      mQuadTree.clear();
    }
  }

  @Override
  public boolean removeItem(T item) {
    // QuadItem delegates hashcode() and equals() to its item so,
    //   removing any QuadItem to that item will remove the item
    boolean remove;
    QuadItem<T> quadItem = new QuadItem<>(item);
    synchronized (this.mQuadTree) {
      remove = this.mItems.remove(quadItem);
      if (remove) {
        this.mQuadTree.remove(quadItem);
      }
    }
    return remove;
  }

  @Override
  public boolean removeItems(Collection<T> collection) {
    boolean z;
    synchronized (this.mQuadTree) {
      z = false;
      for (T t : collection) {
        QuadItem<T> quadItem = new QuadItem<>(t);
        if (this.mItems.remove(quadItem)) {
          this.mQuadTree.remove(quadItem);
          z = true;
        }
      }
    }
    return z;
  }

  @Override
  public boolean updateItem(T item) {
    boolean removeItem;
    synchronized (this.mQuadTree) {
      removeItem = removeItem(item);
      if (removeItem) {
        removeItem = addItem(item);
      }
    }
    return removeItem;
  }


  /**
   * 这是算法核心
   * @param zoom zoom map 的级别
   * @return
   */
  @Override
  public Set<? extends Cluster<T>> getClusters(double zoom) {
    final int discreteZoom = (int) zoom;

    //定义的可进行聚合的距离
    final double zoomSpecificSpan = mMaxDistance / Math.pow(2.0, discreteZoom) / 256.0;

     //遍历QuadItem时保存被遍历过的Item
    final Set<QuadItem<T>> visitedCandidates = new HashSet<>();

    //保存要返回的cluster簇，每个cluster中包含若干个ClusterItem对象
    final Set<Cluster<T>> results = new HashSet<>();

    //Item --> 此Item与所属的cluster中心点的距离
    final Map<QuadItem<T>, Double> distanceToCluster = new HashMap<>();

    //Item对象 --> 此Item所属的cluster
    final Map<QuadItem<T>, StaticCluster<T>> itemToCluster = new HashMap<>();

    synchronized (mQuadTree) {
      //遍历所有的QuadItem
      for (QuadItem<T> candidate : getClusteringItems(mQuadTree, zoom)) {
        if (visitedCandidates.contains(candidate)) {
          ////如果此Item已经被别的cluster框住了，就不再处理它
          // Candidate is already part of another cluster.
          continue;
        }

        //这个就是我们说的，根据给定距离生成一个框框
        Bounds searchBounds = createBoundsFromSpan(candidate.getPoint(), zoomSpecificSpan);

        //search 某边界范围内的clusterItems
        Collection<QuadItem<T>> clusterItems = mQuadTree.search(searchBounds);
        if (clusterItems.size() == 1) {
           // 如果只有一个点，那么这一个点就是一个cluster，QuadItem也实现了Cluster接口，也可以当作Cluster对象
          // Only the current marker is in range. Just add the single item to the results.
          results.add(candidate);
          visitedCandidates.add(candidate);
          distanceToCluster.put(candidate, 0d);
          continue;
        }
        //如果搜索到多个点,那么就以此item为中心创建一个cluster
        StaticCluster<T> cluster = new StaticCluster<T>(candidate.mClusterItem.getPosition());
        results.add(cluster);

        //遍历所有框住的点
        for (QuadItem<T> clusterItem : clusterItems) {
          //获取此item与原来的cluster中心的距离(如果之前已经被其他cluster给框住了)
          Double existingDistance = distanceToCluster.get(clusterItem);

          //获取此item与现在这个cluster中心的距离
          double distance = distanceSquared(clusterItem.getPoint(), candidate.getPoint());
          if (existingDistance != null) {
            // 判断那个距离跟小
            // Item already belongs to another cluster. Check if it's closer to this cluster.
            if (existingDistance < distance) {
              continue;
            }
            //如果跟现在的cluster距离更近，则将此item从原来的cluster中移除
            // Move item to the closer cluster.
            itemToCluster.get(clusterItem).remove(clusterItem.mClusterItem);
          }
          //保存此item到cluster中心的距离
          distanceToCluster.put(clusterItem, distance);
          //将此item添加到cluster中
          cluster.add(clusterItem.mClusterItem);
          //建立item -- cluster 的map
          itemToCluster.put(clusterItem, cluster);
        }
        //将所有框住过的点添加到已访问的List中
        visitedCandidates.addAll(clusterItems);
      }
    }
    return results;
  }

  public Collection<QuadItem<T>> getClusteringItems(
      PointQuadTree<QuadItem<T>> quadTree, double discreteZoom) {
    return mItems;
  }

  @Override
  public Collection<T> getItems() {
    final List<T> items = new ArrayList<T>();
    synchronized (mQuadTree) {
      for (QuadItem<T> quadItem : mItems) {
        items.add(quadItem.mClusterItem);
      }
    }
    return items;
  }

  @Override
  public void setMaxDistanceBetweenClusteredItems(int maxDistance) {
    System.out.println("----setMaxDistanceBetweenClusteredItems------->>>");
    mMaxDistance = maxDistance;
  }

  @Override
  public int getMaxDistanceBetweenClusteredItems() {
    return mMaxDistance;
  }

  private double distanceSquared(Point a, Point b) {
    return (a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y);
  }

  private Bounds createBoundsFromSpan(Point p, double span) {
    // TODO: Use a span that takes into account the visual size of the marker, not just its
    // LatLng.
    double halfSpan = span / 2;
    return new Bounds(p.x - halfSpan, p.x + halfSpan, p.y - halfSpan, p.y + halfSpan);
  }

  static class QuadItem<T extends ClusterItem> implements PointQuadTree.Item, Cluster<T> {
    private final T mClusterItem;
    private final Point mPoint;
    private final LatLng mPosition;
    private Set<T> singletonSet;

    private QuadItem(T item) {
      mClusterItem = item;
      mPosition = item.getPosition();
      mPoint = PROJECTION.toPoint(mPosition);
      singletonSet = Collections.singleton(mClusterItem);
    }

    @Override
    public Point getPoint() {
      return mPoint;
    }

    @Override
    public LatLng getPosition() {
      return mPosition;
    }

    @Override
    public Set<T> getItems() {
      return singletonSet;
    }

    @Override
    public int getSize() {
      return 1;
    }

    @Override
    public int hashCode() {
      return mClusterItem.hashCode();
    }

    @Override
    public boolean equals(Object other) {
      if (!(other instanceof QuadItem<?>)) {
        return false;
      }

      return ((QuadItem<?>) other).mClusterItem.equals(mClusterItem);
    }
  }
}
