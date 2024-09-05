/*
 * Copyright (C) 2015 Baidu, Inc. All Rights Reserved.
 */

package com.baidu.mapapi.clusterutil.clustering;

import android.content.Context;
import android.os.AsyncTask;
import androidx.annotation.Nullable;
import com.baidu.mapapi.clusterutil.clustering.algo.Algorithm;
import com.baidu.mapapi.clusterutil.clustering.algo.NonHierarchicalDistanceBasedAlgorithm;
import com.baidu.mapapi.clusterutil.clustering.algo.PreCachingAlgorithmDecorator;
import com.baidu.mapapi.clusterutil.clustering.algo.ScreenBasedAlgorithm;
import com.baidu.mapapi.clusterutil.clustering.algo.ScreenBasedAlgorithmAdapter;
import com.baidu.mapapi.clusterutil.clustering.view.ClusterRenderer;
import com.baidu.mapapi.clusterutil.clustering.view.DefaultClusterRenderer;
import com.baidu.mapapi.clusterutil.collections.MarkerManager;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.model.LatLngBounds;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Groups many items on a map based on zoom level.
 *
 * <p>ClusterManager should be added to the map
 * <li>
 */
public class ClusterManager<T extends ClusterItem>
    implements BaiduMap.OnMapStatusChangeListener, BaiduMap.OnMarkerClickListener {
  private final MarkerManager mMarkerManager;
  private final MarkerManager.Collection mMarkers;
  private final MarkerManager.Collection mClusterMarkers;

  private ScreenBasedAlgorithm<T> mAlgorithm;

  private ClusterRenderer<T> mRenderer;

  private BaiduMap mMap;
  private MapStatus mPreviousCameraPosition;
  private ClusterTask mClusterTask;
  private final ReadWriteLock mClusterTaskLock = new ReentrantReadWriteLock();

  private OnClusterItemClickListener<T> mOnClusterItemClickListener;
  private OnClusterInfoWindowClickListener<T> mOnClusterInfoWindowClickListener;
  private OnClusterItemInfoWindowClickListener<T> mOnClusterItemInfoWindowClickListener;
  private OnClusterClickListener<T> mOnClusterClickListener;

  public ClusterManager(Context context, BaiduMap map) {
    this(context, map, new MarkerManager(map));
  }

  public ClusterManager(Context context, BaiduMap map, MarkerManager markerManager) {
    mMap = map;
    mMarkerManager = markerManager;
    mClusterMarkers = markerManager.newCollection();
    mMarkers = markerManager.newCollection();
    mRenderer = new DefaultClusterRenderer<T>(context, map, this);
    mAlgorithm =
        new ScreenBasedAlgorithmAdapter<>(
            new PreCachingAlgorithmDecorator<T>(new NonHierarchicalDistanceBasedAlgorithm<T>()));

    mClusterTask = new ClusterTask();
    mRenderer.onAdd();
  }

  public MarkerManager.Collection getMarkerCollection() {
    return mMarkers;
  }

  public MarkerManager.Collection getClusterMarkerCollection() {
    return mClusterMarkers;
  }

  public MarkerManager getMarkerManager() {
    return mMarkerManager;
  }

  /**
   * 通过marker 获取数据
   *
   * @param marker
   * @return
   */
  @Nullable
  public T findClusterMarkerData(Marker marker) {
    return mRenderer.findClusterMarkerData(marker);
  }

  @Nullable
  public Cluster<T> findClusterMarkersData(Marker marker) {
    return mRenderer.findClusterMarkersData(marker);
  }

  public ClusterRenderer<T> getRenderer() {
    return mRenderer;
  }

  public Algorithm<T> getAlgorithm() {
    return mAlgorithm;
  }

  public void setRenderer(ClusterRenderer<T> view) {
    mRenderer.setOnClusterClickListener(null);
    mRenderer.setOnClusterItemClickListener(null);
    mClusterMarkers.clear();
    mMarkers.clear();
    mRenderer.onRemove();
    mRenderer = view;
    mRenderer.onAdd();
    mRenderer.setOnClusterClickListener(mOnClusterClickListener);
    mRenderer.setOnClusterInfoWindowClickListener(mOnClusterInfoWindowClickListener);
    mRenderer.setOnClusterItemClickListener(mOnClusterItemClickListener);
    mRenderer.setOnClusterItemInfoWindowClickListener(mOnClusterItemInfoWindowClickListener);
    cluster();
  }

  public void setAlgorithm(Algorithm<T> algorithm) {
    if (algorithm instanceof ScreenBasedAlgorithm) {
      setAlgorithm((ScreenBasedAlgorithm<T>) algorithm);
    } else {
      setAlgorithm(new ScreenBasedAlgorithmAdapter<>(algorithm));
    }
  }

  public void setAlgorithm(ScreenBasedAlgorithm<T> algorithm) {
    algorithm.lock();
    try {
      final Algorithm<T> oldAlgorithm = getAlgorithm();
      mAlgorithm = algorithm;

      if (oldAlgorithm != null) {
        oldAlgorithm.lock();
        try {
          algorithm.addItems(oldAlgorithm.getItems());
        } finally {
          oldAlgorithm.unlock();
        }
      }
    } finally {
      algorithm.unlock();
    }

    if (mAlgorithm.shouldReclusterOnMapMovement()) {
      mAlgorithm.onMapStatusChange(mMap.getMapStatus());
    }

    cluster();
  }
  /**
   * Removes all items from the cluster manager. After calling this method you must invoke {@link
   * #cluster()} for the map to be cleared.
   */
  public void clearItems() {
    final Algorithm<T> algorithm = getAlgorithm();
    algorithm.lock();
    try {
      algorithm.clearItems();
    } finally {
      algorithm.unlock();
    }
  }

  /**
   * Adds items to clusters. After calling this method you must invoke {@link #cluster()} for the
   * state of the clusters to be updated on the map.
   *
   * @param items items to add to clusters
   * @return true if the cluster manager contents changed as a result of the call
   */
  public boolean addItems(Collection<T> items) {
    final Algorithm<T> algorithm = getAlgorithm();
    algorithm.lock();
    try {
      return algorithm.addItems(items);
    } finally {
      algorithm.unlock();
    }
  }

  /**
   * Adds an item to a cluster. After calling this method you must invoke {@link #cluster()} for the
   * state of the clusters to be updated on the map.
   *
   * @param myItem item to add to clusters
   * @return true if the cluster manager contents changed as a result of the call
   */
  public boolean addItem(T myItem) {
    final Algorithm<T> algorithm = getAlgorithm();
    algorithm.lock();
    try {
      return algorithm.addItem(myItem);
    } finally {
      algorithm.unlock();
    }
  }

  /**
   * Removes items from clusters. After calling this method you must invoke {@link #cluster()} for
   * the state of the clusters to be updated on the map.
   *
   * @param items items to remove from clusters
   * @return true if the cluster manager contents changed as a result of the call
   */
  public boolean removeItems(Collection<T> items) {
    final Algorithm<T> algorithm = getAlgorithm();
    algorithm.lock();
    try {
      return algorithm.removeItems(items);
    } finally {
      algorithm.unlock();
    }
  }

  /**
   * Removes an item from clusters. After calling this method you must invoke {@link #cluster()} for
   * the state of the clusters to be updated on the map.
   *
   * @param item item to remove from clusters
   * @return true if the item was removed from the cluster manager as a result of this call
   */
  public boolean removeItem(T item) {
    final Algorithm<T> algorithm = getAlgorithm();
    algorithm.lock();
    try {
      return algorithm.removeItem(item);
    } finally {
      algorithm.unlock();
    }
  }

  /** Force a re-cluster. You may want to call this after adding new item(s). */
  public void cluster() {
    mClusterTaskLock.writeLock().lock();
    try {
      // Attempt to cancel the in-flight request.
      mClusterTask.cancel(true);
      mClusterTask = new ClusterTask();
      mClusterTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mMap.getMapStatus().zoom);
    } finally {
      mClusterTaskLock.writeLock().unlock();
    }
  }

  /**
   * Updates an item in clusters. After calling this method you must invoke {@link #cluster()} for
   * the state of the clusters to be updated on the map.
   *
   * @param item item to update in clusters
   * @return true if the item was updated in the cluster manager, false if the item is not contained
   *     within the cluster manager and the cluster manager contents are unchanged
   */
  public boolean updateItem(T item) {
    final Algorithm<T> algorithm = getAlgorithm();
    algorithm.lock();
    try {
      return algorithm.updateItem(item);
    } finally {
      algorithm.unlock();
    }
  }

  @Override
  public void onMapStatusChangeStart(MapStatus mapStatus) {

  }

  // @Override
  // public void onMapStatusChangeStart(MapStatus status, int reason) {
  //
  // }

  @Override
  public void onMapStatusChange(MapStatus mapStatus) {
//    if (mRenderer instanceof BaiduMap.OnMapStatusChangeListener) {
//      ((BaiduMap.OnMapStatusChangeListener) mRenderer).onMapStatusChange(mapStatus);
//    }
//
//    mAlgorithm.onMapStatusChange(mMap.getMapStatus());
//
//    // delegate clustering to the algorithm
//    if (mAlgorithm.shouldReclusterOnMapMovement()) {
//      cluster();
//
//      // Don't re-compute clusters if the map has just been panned/tilted/rotated.
//    } else if (mPreviousCameraPosition == null
//            || mPreviousCameraPosition.zoom != mMap.getMapStatus().zoom) {
//      mPreviousCameraPosition = mMap.getMapStatus();
//      cluster();
//
//    }
  }

  @Override
  public void onMapStatusChangeFinish(MapStatus mapStatus) {
    if (mRenderer instanceof BaiduMap.OnMapStatusChangeListener) {
      ((BaiduMap.OnMapStatusChangeListener) mRenderer).onMapStatusChange(mapStatus);
    }

    mAlgorithm.onMapStatusChange(mMap.getMapStatus());

    // delegate clustering to the algorithm
    if (mAlgorithm.shouldReclusterOnMapMovement()) {
      cluster();

      // Don't re-compute clusters if the map has just been panned/tilted/rotated.
    } else if (mPreviousCameraPosition == null
        || mPreviousCameraPosition.zoom != mMap.getMapStatus().zoom) {
      mPreviousCameraPosition = mMap.getMapStatus();
      cluster();

    }
  }

  @Override
  public boolean onMarkerClick(Marker marker) {
    return getMarkerManager().onMarkerClick(marker);
  }

  /**
   * Runs the clustering algorithm in a background thread, then re-paints when results come back.
   */
  private class ClusterTask extends AsyncTask<Float, Void, Set<? extends Cluster<T>>> {
    @Override
    protected Set<? extends Cluster<T>> doInBackground(Float... zoom) {
      final Algorithm<T> algorithm = getAlgorithm();
      algorithm.lock();
      try {
        return algorithm.getClusters(zoom[0]);
      } finally {
        algorithm.unlock();
      }
    }

    @Override
    protected void onPostExecute(Set<? extends Cluster<T>> clusters) {
      mRenderer.onClustersChanged(clusters);
    }
  }

  /**
   * Sets a callback that's invoked when a Cluster is tapped. Note: For this listener to function,
   * the ClusterManager must be added as a click listener to the map.
   */
  public void setOnClusterClickListener(OnClusterClickListener<T> listener) {
    mOnClusterClickListener = listener;
    mRenderer.setOnClusterClickListener(listener);
  }

  /**
   * Sets a callback that's invoked when a Cluster is tapped. Note: For this listener to function,
   * the ClusterManager must be added as a info window click listener to the map.
   */
  public void setOnClusterInfoWindowClickListener(OnClusterInfoWindowClickListener<T> listener) {
    mOnClusterInfoWindowClickListener = listener;
    mRenderer.setOnClusterInfoWindowClickListener(listener);
  }

  /**
   * Sets a callback that's invoked when an individual ClusterItem is tapped. Note: For this
   * listener to function, the ClusterManager must be added as a click listener to the map.
   */
  public void setOnClusterItemClickListener(OnClusterItemClickListener<T> listener) {
    mOnClusterItemClickListener = listener;
    mRenderer.setOnClusterItemClickListener(listener);
  }

  /**
   * Sets a callback that's invoked when an individual ClusterItem's Info Window is tapped. Note:
   * For this listener to function, the ClusterManager must be added as a info window click listener
   * to the map.
   */
  public void setOnClusterItemInfoWindowClickListener(
      OnClusterItemInfoWindowClickListener<T> listener) {
    mOnClusterItemInfoWindowClickListener = listener;
    mRenderer.setOnClusterItemInfoWindowClickListener(listener);
  }

  /** Called when a Cluster is clicked. */
  public interface OnClusterClickListener<T extends ClusterItem> {
    public boolean onClusterClick(Cluster<T> cluster);
  }

  /** Called when a Cluster's Info Window is clicked. */
  public interface OnClusterInfoWindowClickListener<T extends ClusterItem> {
    public void onClusterInfoWindowClick(Cluster<T> cluster);
  }

  /** Called when an individual ClusterItem is clicked. */
  public interface OnClusterItemClickListener<T extends ClusterItem> {
    public boolean onClusterItemClick(T item);
  }

  /** Called when an individual ClusterItem's Info Window is clicked. */
  public interface OnClusterItemInfoWindowClickListener<T extends ClusterItem> {
    public void onClusterItemInfoWindowClick(T item);
  }

  @Override
  public void onMapStatusChangeStart(MapStatus mapStatus, int i) {}

  public void zoomToSpan() {
    if (mMap == null) {
      return;
    }
    Collection<T> items = mAlgorithm.getItems();
    LatLngBounds.Builder builder = new LatLngBounds.Builder();
    for (T item : items) {

        builder.include(item.getPosition());
    }
    mMap.setMapStatus(MapStatusUpdateFactory.newLatLngBounds(builder.build()));
  }
}
