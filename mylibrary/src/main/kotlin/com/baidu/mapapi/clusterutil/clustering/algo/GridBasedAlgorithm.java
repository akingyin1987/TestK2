package com.baidu.mapapi.clusterutil.clustering.algo;

import android.util.LongSparseArray;
import com.baidu.mapapi.clusterutil.clustering.Cluster;
import com.baidu.mapapi.clusterutil.clustering.ClusterItem;
import com.baidu.mapapi.clusterutil.projection.Point;
import com.baidu.mapapi.clusterutil.projection.SphericalMercatorProjection;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Groups markers into a grid.
 *
 * @author: aking @CreateDate: 2022/4/29 11:04 @UpdateUser: 更新者 @UpdateDate: 2022/4/29
 *     11:04 @UpdateRemark: 更新说明 @Version: 1.0
 */
public class GridBasedAlgorithm<T extends ClusterItem> extends AbstractAlgorithm<T> {

  private static final int DEFAULT_GRID_SIZE = 100;

  private int mGridSize = DEFAULT_GRID_SIZE;

  private final Set<T> mItems = Collections.synchronizedSet(new HashSet<T>());

  /**
   * Adds an item to the algorithm
   *
   * @param item the item to be added
   * @return true if the algorithm contents changed as a result of the call
   */
  @Override
  public boolean addItem(T item) {
    return mItems.add(item);
  }

  /**
   * Adds a collection of items to the algorithm
   *
   * @param items the items to be added
   * @return true if the algorithm contents changed as a result of the call
   */
  @Override
  public boolean addItems(Collection<T> items) {
    return mItems.addAll(items);
  }

  @Override
  public void clearItems() {
    mItems.clear();
  }

  /**
   * Removes an item from the algorithm
   *
   * @param item the item to be removed
   * @return true if this algorithm contained the specified element (or equivalently, if this
   *     algorithm changed as a result of the call).
   */
  @Override
  public boolean removeItem(T item) {
    return mItems.remove(item);
  }

  /**
   * Removes a collection of items from the algorithm
   *
   * @param items the items to be removed
   * @return true if this algorithm contents changed as a result of the call
   */
  @Override
  public boolean removeItems(Collection<T> items) {
    return mItems.removeAll(items);
  }

  /**
   * Updates the provided item in the algorithm
   *
   * @param item the item to be updated
   * @return true if the item existed in the algorithm and was updated, or false if the item did not
   *     exist in the algorithm and the algorithm contents remain unchanged.
   */
  @Override
  public boolean updateItem(T item) {
    boolean result;
    synchronized (mItems) {
      result = removeItem(item);
      if (result) {
        // Only add the item if it was removed (to help prevent accidental duplicates on map)
        result = addItem(item);
      }
    }
    return result;
  }

  @Override
  public void setMaxDistanceBetweenClusteredItems(int maxDistance) {
    mGridSize = maxDistance;
  }

  @Override
  public int getMaxDistanceBetweenClusteredItems() {
    return mGridSize;
  }

  @Override
  public Set<? extends Cluster<T>> getClusters(double zoom) {
    long numCells = (long) Math.ceil(256 * Math.pow(2, zoom) / mGridSize);
    SphericalMercatorProjection proj = new SphericalMercatorProjection(numCells);

    HashSet<Cluster<T>> clusters = new HashSet<Cluster<T>>();


    return clusters;
  }

  @Override
  public Collection<T> getItems() {
    return mItems;
  }

  private static long getCoord(long numCells, double x, double y) {
    return (long) (numCells * Math.floor(x) + Math.floor(y));
  }
}
