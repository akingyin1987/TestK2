package com.baidu.mapapi.clusterutil.clustering.algo;

import com.baidu.mapapi.clusterutil.clustering.ClusterItem;
import com.baidu.mapapi.clusterutil.projection.Bounds;
import com.baidu.mapapi.clusterutil.projection.Point;
import com.baidu.mapapi.clusterutil.projection.SphericalMercatorProjection;
import com.baidu.mapapi.clusterutil.quadtree.PointQuadTree;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.model.LatLng;
import java.util.ArrayList;
import java.util.Collection;

/**
 * This algorithm works the same way as {@link NonHierarchicalDistanceBasedAlgorithm} but works,
 * only in * visible area. It requires to be reclustered on camera movement because clustering is
 * done only for visible area.
 *
 * @author: aking @CreateDate: 2022/4/29 10:51 @UpdateUser: 更新者 @UpdateDate: 2022/4/29
 *     10:51 @UpdateRemark: 更新说明 @Version: 1.0
 */
public class NonHierarchicalViewBasedAlgorithm<T extends ClusterItem>
    extends NonHierarchicalDistanceBasedAlgorithm<T> implements ScreenBasedAlgorithm<T> {

  private static final SphericalMercatorProjection PROJECTION = new SphericalMercatorProjection(1);

  private int mViewWidth;
  private int mViewHeight;

  private LatLng mMapCenter;

  /**
   * @param screenWidth map width in dp
   * @param screenHeight map height in dp
   */
  public NonHierarchicalViewBasedAlgorithm(int screenWidth, int screenHeight) {
    mViewWidth = screenWidth;
    mViewHeight = screenHeight;
  }

  @Override
  public void onMapStatusChange(MapStatus mapStatus) {
    mMapCenter = mapStatus.target;
  }

  @Override
  public Collection<QuadItem<T>> getClusteringItems(
      PointQuadTree<QuadItem<T>> quadTree, double discreteZoom) {
    Bounds visibleBounds = getVisibleBounds(discreteZoom);
    Collection<QuadItem<T>> items = new ArrayList<>();

    // Handle wrapping around international date line
    if (visibleBounds.minX < 0) {
      Bounds wrappedBounds =
          new Bounds(visibleBounds.minX + 1, 1, visibleBounds.minY, visibleBounds.maxY);
      items.addAll(quadTree.search(wrappedBounds));
      visibleBounds = new Bounds(0, visibleBounds.maxX, visibleBounds.minY, visibleBounds.maxY);
    }
    if (visibleBounds.maxX > 1) {
      Bounds wrappedBounds =
          new Bounds(0, visibleBounds.maxX - 1, visibleBounds.minY, visibleBounds.maxY);
      items.addAll(quadTree.search(wrappedBounds));
      visibleBounds = new Bounds(visibleBounds.minX, 1, visibleBounds.minY, visibleBounds.maxY);
    }
    items.addAll(quadTree.search(visibleBounds));

    return items;
  }

  @Override
  public boolean shouldReclusterOnMapMovement() {
    return true;
  }

  /**
   * Update view width and height in case map size was changed. You need to recluster all the
   * clusters, to update view state after view size changes.
   *
   * @param width map width in dp
   * @param height map height in dp
   */
  public void updateViewSize(int width, int height) {
    mViewWidth = width;
    mViewHeight = height;
  }

  private Bounds getVisibleBounds(double zoom) {
    if (mMapCenter == null) {
      return new Bounds(0, 0, 0, 0);
    }

    Point p = PROJECTION.toPoint(mMapCenter);

    final double halfWidthSpan = mViewWidth / Math.pow(2, zoom) / 256 / 2;
    final double halfHeightSpan = mViewHeight / Math.pow(2, zoom) / 256 / 2;

    return new Bounds(
        p.x - halfWidthSpan, p.x + halfWidthSpan,
        p.y - halfHeightSpan, p.y + halfHeightSpan);
  }
}
