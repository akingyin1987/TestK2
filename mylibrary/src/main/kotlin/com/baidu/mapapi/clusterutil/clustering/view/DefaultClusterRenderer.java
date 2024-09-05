/*
 * Copyright (C) 2015 Baidu, Inc. All Rights Reserved.
 */

package com.baidu.mapapi.clusterutil.clustering.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue;
import android.util.SparseArray;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.baidu.mapapi.clusterutil.clustering.Cluster;
import com.baidu.mapapi.clusterutil.clustering.ClusterItem;
import com.baidu.mapapi.clusterutil.clustering.ClusterManager;
import com.baidu.mapapi.clusterutil.collections.MarkerManager;
import com.baidu.mapapi.clusterutil.projection.Point;
import com.baidu.mapapi.clusterutil.projection.SphericalMercatorProjection;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.Projection;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.model.LatLngBounds;



import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The default view for a ClusterManager. Markers are animated in and out of clusters.
 * 默认的聚合点视频
 * @author zlcd
 */
public class DefaultClusterRenderer<T extends ClusterItem> implements ClusterRenderer<T> {
//  private static final boolean SHOULD_ANIMATE =
//      Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
  private final BaiduMap mMap;
  private final ClusterManager<T> mClusterManager;
  private final float mDensity;
  private boolean mAnimate;
  private long mAnimationDurationMs;
  private final Executor mExecutor = Executors.newSingleThreadExecutor();
  private static final int[] BUCKETS = {10, 20, 50, 100, 200, 500, 1000};
  private ShapeDrawable mColoredCircleBackground;

  /** Markers that are currently on the map. 当前地图上的marks */
  private Set<MarkerWithPosition> mMarkers =
      Collections.newSetFromMap(new ConcurrentHashMap<MarkerWithPosition, Boolean>());

  /**
   * Icons for each bucket
   * 每个块的图标
   */
  private SparseArray<BitmapDescriptor> mIcons = new SparseArray<>();

  /**
   * Markers for single ClusterItems.
   */
  private MarkerCache<T> mMarkerCache = new MarkerCache<>();

  /**
   * If cluster size is less than this size, display individual markers.
   * 最小生成聚合点的marker 数，即当前“聚合点”内marker数小于当前值，则不会以聚合点的形式显示
   * */
 // private int mMinClusterSize = 4;
  private int mMinClusterSize = 4;
  /** The currently displayed set of clusters. */
  private Set<? extends Cluster<T>> mClusters;

  /** Markers for Clusters. */
  private MarkerCache<Cluster<T>> mClusterMarkerCache = new MarkerCache<>();
  //    /**
  //     * Lookup between markers and the associated cluster.
  //     */
  //    private Map<Marker, Cluster<T>> mMarkerToCluster = new HashMap<Marker, Cluster<T>>();
  //    private Map<Cluster<T>, Marker> mClusterToMarker = new HashMap<Cluster<T>, Marker>();

  /** The target zoom level for the current set of clusters. */
  private float mZoom;

  private final ViewModifier mViewModifier = new ViewModifier();

  private ClusterManager.OnClusterClickListener<T> mClickListener;
  private ClusterManager.OnClusterInfoWindowClickListener<T> mInfoWindowClickListener;
  private ClusterManager.OnClusterItemClickListener<T> mItemClickListener;
  private ClusterManager.OnClusterItemInfoWindowClickListener<T> mItemInfoWindowClickListener;

  public DefaultClusterRenderer(Context context, BaiduMap map, ClusterManager<T> clusterManager) {
    mMap = map;
    mAnimate = true;
    mAnimationDurationMs = 300;
    mDensity = context.getResources().getDisplayMetrics().density;

    mClusterManager = clusterManager;
  }

  @Nullable
  @Override
  public T findClusterMarkerData(Marker marker) {
    return mMarkerCache.get(marker);
  }

  @Nullable
  @Override
  public Set<T> findClusterSingleMarkerDatas() {
    return mMarkerCache.mCache.keySet();
  }

  @Nullable
  @Override
  public Set<Cluster<T>> findClusterMarkerDatas() {
    return mClusterMarkerCache.mCache.keySet();
  }

  @Nullable
  @Override
  public Cluster<T> findClusterMarkersData(Marker marker) {
    return mClusterMarkerCache.get(marker);
  }

  @Override
  public void setAnimation(boolean animate) {
    this.mAnimate = animate;
  }

  @Override
  public void onAdd() {
    //添加marker 点击事件
    mClusterManager
        .getMarkerCollection()
        .setOnMarkerClickListener(
            new BaiduMap.OnMarkerClickListener() {
              @Override
              public boolean onMarkerClick(Marker marker) {

                return mItemClickListener != null
                    && mItemClickListener.onClusterItemClick(mMarkerCache.get(marker));
              }
            });

    //添加聚合点点击事件
    mClusterManager
        .getClusterMarkerCollection()
        .setOnMarkerClickListener(
            new BaiduMap.OnMarkerClickListener() {
              @Override
              public boolean onMarkerClick(Marker marker) {
                return mClickListener != null
                    && mClickListener.onClusterClick(mClusterMarkerCache.get(marker));
              }
            });
  }

  @Override
  public void onRemove() {
    //取消监听事件
    mClusterManager.getMarkerCollection().setOnMarkerClickListener(null);
    mClusterManager.getClusterMarkerCollection().setOnMarkerClickListener(null);
  }

  /**
   * 创建聚合点背景
   * @return
   */
  private LayerDrawable makeClusterBackground() {
    mColoredCircleBackground = new ShapeDrawable(new OvalShape());
    ShapeDrawable outline = new ShapeDrawable(new OvalShape());
    outline.getPaint().setColor(0x80ffffff); // Transparent white.
    LayerDrawable background =
        new LayerDrawable(new Drawable[] {outline, mColoredCircleBackground});
    int strokeWidth = (int) (mDensity * 3);
    background.setLayerInset(1, strokeWidth, strokeWidth, strokeWidth, strokeWidth);
    return background;
  }



  @Override
  public int getClusterTextAppearance(int clusterSize) {
    return 0;
  }

  @Override
  public int getColor(int clusterSize) {
    final float hueRange = 220;
    final float sizeRange = 300;
    final float size = Math.min(clusterSize, sizeRange);
    final float hue = (sizeRange - size) * (sizeRange - size) / (sizeRange * sizeRange) * hueRange;
    return Color.HSVToColor(new float[] {hue, 1f, .6f});
  }

  protected String getClusterText(int bucket) {
    if (bucket < BUCKETS[0]) {
      return String.valueOf(bucket);
    }
    return bucket + "+";
  }
  /**
   * Gets the minimum cluster size used to render clusters. For example, if "4" is returned, then
   * for any clusters of size 3 or less the items will be rendered as individual markers instead of
   * as a single cluster marker.
   *
   * @return the minimum cluster size used to render clusters. For example, if "4" is returned, then
   *     for any clusters of size 3 or less the items will be rendered as individual markers instead
   *     of as a single cluster marker.
   */
  public int getMinClusterSize() {
    return mMinClusterSize;
  }

  /**
   * Sets the minimum cluster size used to render clusters. For example, if "4" is provided, then
   * for any clusters of size 3 or less the items will be rendered as individual markers instead of
   * as a single cluster marker.
   *
   * @param minClusterSize the minimum cluster size used to render clusters. For example, if "4" is
   *     provided, then for any clusters of size 3 or less the items will be rendered as individual
   *     markers instead of as a single cluster marker.
   */
  public void setMinClusterSize(int minClusterSize) {
    mMinClusterSize = minClusterSize;
  }

  /**
   * Gets the "bucket" for a particular cluster. By default, uses the number of points within the
   * cluster, bucketed to some set points.
   */
  protected int getBucket(Cluster<T> cluster) {
    int size = cluster.getSize();
    if (size <= BUCKETS[0]) {
      return size;
    }
    for (int i = 0; i < BUCKETS.length - 1; i++) {
      if (size < BUCKETS[i + 1]) {
        return BUCKETS[i];
      }
    }
    return BUCKETS[BUCKETS.length - 1];
  }

  /**
   * 确保同时只一个对视图 进行渲染，并安排 重新渲染任务
   * ViewModifier ensures only one re-rendering of the view occurs at a time, and schedules
   * re-rendering, which is performed by the RenderTask.
   */
  @SuppressLint("HandlerLeak")
  private class ViewModifier extends Handler {
    //执行标志
    private static final int RUN_TASK = 0;
    //执行完成标志
    private static final int TASK_FINISHED = 1;
    //是否正在执行中
    private boolean mViewModificationInProgress = false;
    //安排的下一个任务
    private RenderTask mNextClusters = null;

    @Override
    public void handleMessage(Message msg) {
      if (msg.what == TASK_FINISHED) {
        //当前任务执行完成
        mViewModificationInProgress = false;
        if (mNextClusters != null) {
          // Run the task that was queued up.
          // 通知执行下一个任务
          sendEmptyMessage(RUN_TASK);
        }
        return;
      }
      removeMessages(RUN_TASK);

      if (mViewModificationInProgress) {
        // Busy - wait for the callback.
        return;
      }

      if (mNextClusters == null) {
        // Nothing to do.
        return;
      }

      RenderTask renderTask;
      synchronized (this) {
        renderTask = mNextClusters;
        mNextClusters = null;
        mViewModificationInProgress = true;
      }

      renderTask.setCallback(
          new Runnable() {
            @Override
            public void run() {
              //任务执行完后，通知
              sendEmptyMessage(TASK_FINISHED);
            }
          });
      renderTask.setProjection(mMap.getProjection());
      renderTask.setMapZoom(mMap.getMapStatus().zoom);
      mExecutor.execute(renderTask);

    }

    public void queue(Set<? extends Cluster<T>> clusters) {
      //添加新的任务
      synchronized (this) {
        // Overwrite any pending cluster tasks - we don't care about intermediate states.
        mNextClusters = new RenderTask(clusters);
      }
      sendEmptyMessage(RUN_TASK);
    }
  }

  /**
   * Determine whether the cluster should be rendered as individual markers or a cluster.
   * @param cluster cluster to examine for rendering
   * @return true if the provided cluster should be rendered as a single marker on the map, false
   * if the items within this cluster should be rendered as individual markers instead.
   */
  protected boolean shouldRenderAsCluster(Cluster<T> cluster) {
    return cluster.getSize() > mMinClusterSize;
  }

  /**
   * Determines if the new clusters should be rendered on the map, given the old clusters. This
   * method is primarily for optimization of performance, and the default implementation simply
   * checks if the new clusters are equal to the old clusters, and if so, it returns false.
   *
   * However, there are cases where you may want to re-render the clusters even if they didn't
   * change. For example, if you want a cluster with one item to render as a cluster above
   * a certain zoom level and as a marker below a certain zoom level (even if the contents of the
   * clusters themselves did not change). In this case, you could check the zoom level in an
   * implementation of this method and if that zoom level threshold is crossed return true, else
   * {@code return super.shouldRender(oldClusters, newClusters)}.
   *
   * Note that always returning true from this method could potentially have negative performance
   * implications as clusters will be re-rendered on each pass even if they don't change.
   *
   * @param oldClusters The clusters from the previous iteration of the clustering algorithm
   * @param newClusters The clusters from the current iteration of the clustering algorithm
   * @return true if the new clusters should be rendered on the map, and false if they should not. This
   *      method is primarily for optimization of performance, and the default implementation simply
   *      checks if the new clusters are equal to the old clusters, and if so, it returns false.
   */
  protected boolean shouldRender(@NonNull Set<? extends Cluster<T>> oldClusters, @NonNull Set<? extends Cluster<T>> newClusters) {
    return !newClusters.equals(oldClusters);
  }

  /**
   *
   * 这个函数的主要目的是将当前视图（由DefaultClusterRenderer.mClusters和DefaultClusterRenderer.mZoom表示）转换为一个新的缩放级别和一组新的聚合点
   * 这个函数需要在用户界面(UI)线程之外运行。工作在RenderTask中进行协调，然后被添加到MarkerModifier的队列中以供执行
   * 这个函数有三个阶段来进行渲染：
   * 将标记添加到地图上。
   * 将标记动画化到它们的最终位置。
   * 移除地图上任何旧的标记
   * Transforms the current view (represented by DefaultClusterRenderer.mClusters and
   * DefaultClusterRenderer.mZoom) to a new zoom level and set of clusters.
   *
   * <p>This must be run off the UI thread. Work is coordinated in the RenderTask, then queued up to
   * be executed by a MarkerModifier.
   *
   * <p>There are three stages for the render:
   *
   * <p>1. Markers are added to the map
   *
   * <p>2. Markers are animated to their final position
   *
   * <p>3. Any old markers are removed from the map
   *
   * <p>When zooming in, markers are animated out from the nearest existing cluster. When zooming
   * out, existing clusters are animated to the nearest new cluster.
   */
  private class RenderTask implements Runnable {
    final Set<? extends Cluster<T>> clusters;
    private Runnable mCallback;
    private Projection mProjection;
    private SphericalMercatorProjection mSphericalMercatorProjection;
    private float mMapZoom;

    private RenderTask(Set<? extends Cluster<T>> clusters) {
      this.clusters = clusters;
    }

    /**
     * A callback to be run when all work has been completed.
     *
     * @param callback
     */
    public void setCallback(Runnable callback) {
      mCallback = callback;
    }

    public void setProjection(Projection projection) {
      this.mProjection = projection;
    }

    public void setMapZoom(float zoom) {
      this.mMapZoom = zoom;
      this.mSphericalMercatorProjection =
          new SphericalMercatorProjection(256 * Math.pow(2, Math.min(zoom, mZoom)));
    }

    @SuppressLint("NewApi")
    @Override
    public void run() {
      if (!shouldRender(immutableOf(DefaultClusterRenderer.this.mClusters), immutableOf(clusters))) {
        mCallback.run();
        return;
      }

      final MarkerModifier markerModifier = new MarkerModifier();

      final float zoom = mMapZoom;
      final boolean zoomingIn = zoom > mZoom;
      final float zoomDelta = zoom - mZoom;

      final Set<MarkerWithPosition> markersToRemove = mMarkers;
      //// Prevent crashes: https://issuetracker.google.com/issues/35827242
      LatLngBounds visibleBounds;
      try {
        visibleBounds = mMap.getMapStatus().bound;
      }catch (Exception e){
        e.printStackTrace();
        visibleBounds = new  LatLngBounds.Builder().include(new LatLng(0.0,0.0)).build();
      }


      // TODO: Add some padding, so that markers can animate in from off-screen.
      // 当聚合点添加动画
      // Find all of the existing clusters that are on-screen. These are candidates for
      // markers to animate from.
      List<Point> existingClustersOnScreen = null;


      // Create the new markers and animate them to their new positions.
      final Set<MarkerWithPosition> newMarkers = Collections.newSetFromMap(new ConcurrentHashMap<MarkerWithPosition, Boolean>());


      // Wait for all markers to be added.
      markerModifier.waitUntilFree();

      // Don't remove any markers that were just added. This is basically anything that had
      // a hit in the MarkerCache.
      markersToRemove.removeAll(newMarkers);

      // Find all of the new clusters that were added on-screen. These are candidates for
      // markers to animate from.
      List<Point> newClustersOnScreen = null;
      //SHOULD_ANIMATE && mAnimate


      // Remove the old markers, animating them into clusters if zooming out.
      for (final MarkerWithPosition marker : markersToRemove) {
        boolean onScreen = visibleBounds.contains(marker.position);
        // Don't animate when zooming out more than 3 zoom levels.
        // TODO: drop animation based on speed of device & number of markers to animate.
        if (!zoomingIn && zoomDelta > -3 && onScreen  && mAnimate) {
          final Point point = mSphericalMercatorProjection.toPoint(marker.position);
          final Point closest = findClosestCluster(newClustersOnScreen, point);
          if (closest != null) {
            LatLng animateTo = mSphericalMercatorProjection.toLatLng(closest);
            markerModifier.animateThenRemove(marker, marker.position, animateTo);
          } else {
            markerModifier.remove(true, marker.marker);
          }
        } else {
          markerModifier.remove(onScreen, marker.marker);
        }
      }

      markerModifier.waitUntilFree();

      mMarkers = newMarkers;
      DefaultClusterRenderer.this.mClusters = clusters;
      mZoom = zoom;

      mCallback.run();
    }
  }
  /**
   * {@inheritDoc} The default duration is 300 milliseconds.
   * @param animationDurationMs long: The length of the animation, in milliseconds. This value cannot be negative.
   */
  @Override
  public void setAnimationDuration(long animationDurationMs) {
    mAnimationDurationMs = animationDurationMs;
  }
  @Override
  public void onClustersChanged(Set<? extends Cluster<T>> clusters) {
    mViewModifier.queue(clusters);
  }

  @Override
  public void setOnClusterClickListener(ClusterManager.OnClusterClickListener<T> listener) {
    mClickListener = listener;
  }

  @Override
  public void setOnClusterInfoWindowClickListener(
      ClusterManager.OnClusterInfoWindowClickListener<T> listener) {
    mInfoWindowClickListener = listener;
  }

  @Override
  public void setOnClusterItemClickListener(ClusterManager.OnClusterItemClickListener<T> listener) {
    mItemClickListener = listener;
  }
  private Set<? extends Cluster<T>> immutableOf(Set<? extends Cluster<T>> clusters) {
    return clusters != null ? Collections.unmodifiableSet(clusters) : Collections.emptySet();
  }


  private Point findClosestCluster(List<Point> markers, Point point) {
    if (markers == null || markers.isEmpty()) {
      return null;
    }

    int maxDistance = mClusterManager.getAlgorithm().getMaxDistanceBetweenClusteredItems();
    double minDistSquared = maxDistance * maxDistance;
    Point closest = null;
    for (Point candidate : markers) {
      double dist = distanceSquared(candidate, point);
      if (dist < minDistSquared) {
        closest = candidate;
        minDistSquared = dist;
      }
    }
    return closest;
  }

  @Override
  public void setOnClusterItemInfoWindowClickListener(
      ClusterManager.OnClusterItemInfoWindowClickListener<T> listener) {
    mItemInfoWindowClickListener = listener;
  }

  private static double distanceSquared(Point a, Point b) {
    return (a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y);
  }

  /**
   * Handles all markerWithPosition manipulations on the map. Work (such as adding, removing, or
   * animating a markerWithPosition) is performed while trying not to block the rest of the app's
   * UI.
   */
  @SuppressLint("HandlerLeak")
  private class MarkerModifier extends Handler implements MessageQueue.IdleHandler {
    private static final int BLANK = 0;

    private final Lock lock = new ReentrantLock();
    private final Condition busyCondition = lock.newCondition();

    private Queue<CreateMarkerTask> mCreateMarkerTasks = new LinkedList<CreateMarkerTask>();
    private Queue<CreateMarkerTask> mOnScreenCreateMarkerTasks = new LinkedList<CreateMarkerTask>();
    private Queue<Marker> mRemoveMarkerTasks = new LinkedList<Marker>();
    private Queue<Marker> mOnScreenRemoveMarkerTasks = new LinkedList<Marker>();
    private Queue<AnimationTask> mAnimationTasks = new LinkedList<AnimationTask>();

    /** Whether the idle listener has been added to the UI thread's MessageQueue. */
    private boolean mListenerAdded;

    private MarkerModifier() {
      super(Looper.getMainLooper());
    }

    /**
     * Creates markers for a cluster some time in the future.
     *
     * @param priority whether this operation should have priority.
     */
    public void add(boolean priority, CreateMarkerTask c) {
      lock.lock();
      try {
        sendEmptyMessage(BLANK);
        if (priority) {
          mOnScreenCreateMarkerTasks.add(c);
        } else {
          mCreateMarkerTasks.add(c);
        }
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        lock.unlock();
      }
    }

    /**
     * Removes a markerWithPosition some time in the future.
     *
     * @param priority whether this operation should have priority.
     * @param m the markerWithPosition to remove.
     */
    public void remove(boolean priority, Marker m) {
      lock.lock();
      try {
        sendEmptyMessage(BLANK);
        if (priority) {
          mOnScreenRemoveMarkerTasks.add(m);
        } else {
          mRemoveMarkerTasks.add(m);
        }
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        lock.unlock();
      }
    }

    /**
     * Animates a markerWithPosition some time in the future.
     *
     * @param marker the markerWithPosition to animate.
     * @param from the position to animate from.
     * @param to the position to animate to.
     */
    public void animate(MarkerWithPosition marker, LatLng from, LatLng to) {
      lock.lock();
      try {

        mAnimationTasks.add(new AnimationTask(marker, from, to));
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        lock.unlock();
      }
    }

    /**
     * Animates a markerWithPosition some time in the future, and removes it when the animation is
     * complete.
     *
     * @param marker the markerWithPosition to animate.
     * @param from the position to animate from.
     * @param to the position to animate to.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void animateThenRemove(MarkerWithPosition marker, LatLng from, LatLng to) {
      lock.lock();
      try {
        AnimationTask animationTask = new AnimationTask(marker, from, to);
        animationTask.removeOnAnimationComplete(mClusterManager.getMarkerManager());
        mAnimationTasks.add(animationTask);
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        lock.unlock();
      }
    }

    @Override
    public void handleMessage(Message msg) {
      if (!mListenerAdded) {
        Looper.myQueue().addIdleHandler(this);
        mListenerAdded = true;
      }
      removeMessages(BLANK);

      lock.lock();
      try {

        // Perform up to 10 tasks at once.
        // Consider only performing 10 remove tasks, not adds and animations.
        // Removes are relatively slow and are much better when batched.
        for (int i = 0; i < 10; i++) {
          performNextTask();
        }

        if (!isBusy()) {
          mListenerAdded = false;
          Looper.myQueue().removeIdleHandler(this);
          // Signal any other threads that are waiting.
          busyCondition.signalAll();
        } else {
          // Sometimes the idle queue may not be called - schedule up some work regardless
          // of whether the UI thread is busy or not.
          // TODO: try to remove this.
          sendEmptyMessageDelayed(BLANK, 10);
        }
      } finally {
        lock.unlock();
      }
    }

    /** Perform the next task. Prioritise any on-screen work. */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void performNextTask() {
      if (!mOnScreenRemoveMarkerTasks.isEmpty()) {
        removeMarker(mOnScreenRemoveMarkerTasks.poll());
      } else if (!mAnimationTasks.isEmpty()) {
        mAnimationTasks.poll().perform();
      } else if (!mOnScreenCreateMarkerTasks.isEmpty()) {
        mOnScreenCreateMarkerTasks.poll().perform(this);
      } else if (!mCreateMarkerTasks.isEmpty()) {
        mCreateMarkerTasks.poll().perform(this);
      } else if (!mRemoveMarkerTasks.isEmpty()) {
        removeMarker(mRemoveMarkerTasks.poll());
      }
    }

    private void removeMarker(Marker m) {
      mClusterMarkerCache.remove(m);
      mMarkerCache.remove(m);

      mClusterManager.getMarkerManager().remove(m);
    }

    /**
     * @return true if there is still work to be processed.
     */
    public boolean isBusy() {
      lock.lock();
      try {

        return !(mCreateMarkerTasks.isEmpty()
            && mOnScreenCreateMarkerTasks.isEmpty()
            && mOnScreenRemoveMarkerTasks.isEmpty()
            && mRemoveMarkerTasks.isEmpty()
            && mAnimationTasks.isEmpty());
      } finally {
        lock.unlock();
      }
    }

    /** Blocks the calling thread until all work has been processed. */
    public void waitUntilFree() {
      while (isBusy()) {
        // Sometimes the idle queue may not be called - schedule up some work regardless
        // of whether the UI thread is busy or not.
        // TODO: try to remove this.
        sendEmptyMessage(BLANK);
        lock.lock();
        try {
          if (isBusy()) {
            busyCondition.await();
          }
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        } finally {
          lock.unlock();
        }
      }
    }

    @Override
    public boolean queueIdle() {
      // When the UI is not busy, schedule some work.
      sendEmptyMessage(BLANK);
      return true;
    }
  }

  /** A cache of markers representing individual ClusterItems. */
  private static class MarkerCache<T> {
    private Map<T, Marker> mCache = new HashMap<T, Marker>();
    private Map<Marker, T> mCacheReverse = new HashMap<Marker, T>();

    public Marker get(T item) {
      return mCache.get(item);
    }

    public T get(Marker m) {
      return mCacheReverse.get(m);
    }

    public void put(T item, Marker m) {
      mCache.put(item, m);
      mCacheReverse.put(m, item);
    }

    public void remove(Marker m) {
      T item = mCacheReverse.get(m);
      mCacheReverse.remove(m);
      mCache.remove(item);
    }
  }

  /**
   * Called before the marker for a ClusterItem is added to the map. The default implementation
   * sets the marker and snippet text based on the respective item text if they are both
   * available, otherwise it will set the title if available, and if not it will set the marker
   * title to the item snippet text if that is available.
   *
   * The first time {@link ClusterManager#cluster()} is invoked on a set of items
   * {@link #onBeforeClusterItemRendered(ClusterItem, MarkerOptions)} will be called and
   * {@link #onClusterItemUpdated(ClusterItem, Marker)} will not be called.
   * If an item is removed and re-added (or updated) and {@link ClusterManager#cluster()} is
   * invoked again, then {@link #onClusterItemUpdated(ClusterItem, Marker)} will be called and
   * {@link #onBeforeClusterItemRendered(ClusterItem, MarkerOptions)} will not be called.
   *
   * @param item item to be rendered
   * @param markerOptions the markerOptions representing the provided item
   */
  protected void onBeforeClusterItemRendered(T item, MarkerOptions markerOptions) {

  }

  /**
   * Called when a cached marker for a ClusterItem already exists on the map so the marker may
   * be updated to the latest item values. Default implementation updates the title and snippet
   * of the marker if they have changed and refreshes the info window of the marker if it is open.
   * Note that the contents of the item may not have changed since the cached marker was created -
   * implementations of this method are responsible for checking if something changed (if that
   * matters to the implementation).
   *
   * The first time {@link ClusterManager#cluster()} is invoked on a set of items
   * {@link #onBeforeClusterItemRendered(ClusterItem, MarkerOptions)} will be called and
   * {@link #onClusterItemUpdated(ClusterItem, Marker)} will not be called.
   * If an item is removed and re-added (or updated) and {@link ClusterManager#cluster()} is
   * invoked again, then {@link #onClusterItemUpdated(ClusterItem, Marker)} will be called and
   * {@link #onBeforeClusterItemRendered(ClusterItem, MarkerOptions)} will not be called.
   *
   * @param item item being updated
   * @param marker cached marker that contains a potentially previous state of the item.
   */
  protected void onClusterItemUpdated(@NonNull T item, @NonNull Marker marker) {

  }



  /**
   * Called before the marker for a Cluster is added to the map. The default implementation draws a
   * circle with a rough count of the number of items.
   */
  protected void onBeforeClusterRendered(Cluster<T> cluster, MarkerOptions markerOptions) {

    // TODO: consider adding anchor(.5, .5) (Individual markers will overlap more often)
    markerOptions.icon(getDescriptorForCluster(cluster));
  }

  /**
   * Gets a BitmapDescriptor for the given cluster that contains a rough count of the number of
   * items. Used to set the cluster marker icon in the default implementations of
   * {@link #onBeforeClusterRendered(Cluster, MarkerOptions)} and
   * {@link #onClusterUpdated(Cluster, Marker)}.
   *
   * @param cluster cluster to get BitmapDescriptor for
   * @return a BitmapDescriptor for the marker icon for the given cluster that contains a rough
   * count of the number of items.
   */
  @NonNull
  protected BitmapDescriptor getDescriptorForCluster(@NonNull Cluster<T> cluster) {
    int bucket = getBucket(cluster);
    BitmapDescriptor descriptor = mIcons.get(bucket);

    return descriptor;
  }



  /** Called after the marker for a Cluster has been added to the map. */
  protected void onClusterRendered(Cluster<T> cluster, Marker marker) {}

  /** Called after the marker for a ClusterItem has been added to the map. */
  protected void onClusterItemRendered(T clusterItem, Marker marker) {}


  /**
   * Called when a cached marker for a Cluster already exists on the map so the marker may
   * be updated to the latest cluster values. Default implementation updated the icon with a
   * circle with a rough count of the number of items. Note that the contents of the cluster may
   * not have changed since the cached marker was created - implementations of this method are
   * responsible for checking if something changed (if that matters to the implementation).
   *
   * The first time {@link ClusterManager#cluster()} is invoked on a set of items
   * {@link #onBeforeClusterRendered(Cluster, MarkerOptions)} will be called and
   * {@link #onClusterUpdated(Cluster, Marker)} will not be called. If an item is removed and
   * re-added (or updated) and {@link ClusterManager#cluster()} is invoked
   * again, then {@link #onClusterUpdated(Cluster, Marker)} will be called and
   * {@link #onBeforeClusterRendered(Cluster, MarkerOptions)} will not be called.
   *
   * @param cluster cluster being updated
   * @param marker cached marker that contains a potentially previous state of the cluster
   */
  protected void onClusterUpdated(@NonNull Cluster<T> cluster, @NonNull Marker marker) {
    // TODO: consider adding anchor(.5, .5) (Individual markers will overlap more often)
    marker.setIcon(getDescriptorForCluster(cluster));
  }

  /**
   * Get the marker from a ClusterItem
   *
   * @param clusterItem ClusterItem which you will obtain its marker
   * @return a marker from a ClusterItem or null if it does not exists
   */
  public Marker getMarker(T clusterItem) {
    return mMarkerCache.get(clusterItem);
  }

  /**
   * Get the marker from a Cluster
   *
   * @param cluster which you will obtain its marker
   * @return a marker from a cluster or null if it does not exists
   */
  public Marker getMarker(Cluster<T> cluster) {
    return mClusterMarkerCache.get(cluster);
  }

  /**
   * Get the ClusterItem from a marker
   *
   * @param marker which you will obtain its ClusterItem
   * @return a ClusterItem from a marker or null if it does not exists
   */
  public T getClusterItem(Marker marker) {
    return mMarkerCache.get(marker);
  }

  /**
   * Get the Cluster from a marker
   *
   * @param marker which you will obtain its Cluster
   * @return a Cluster from a marker or null if it does not exists
   */
  public Cluster<T> getCluster(Marker marker) {
    return mClusterMarkerCache.get(marker);
  }

  /** Creates markerWithPosition(s) for a particular cluster, animating it if necessary. */
  private class CreateMarkerTask {
    private final Cluster<T> cluster;
    private final Set<MarkerWithPosition> newMarkers;
    private final LatLng animateFrom;

    /**
     * @param c the cluster to render.
     * @param markersAdded a collection of markers to append any created markers.
     * @param animateFrom the location to animate the markerWithPosition from, or null if no
     *     animation is required.
     */
    public CreateMarkerTask(
        Cluster<T> c, Set<MarkerWithPosition> markersAdded, LatLng animateFrom) {
      this.cluster = c;
      this.newMarkers = markersAdded;
      this.animateFrom = animateFrom;
    }

    private void perform(MarkerModifier markerModifier) {
      // Don't show small clusters. Render the markers inside, instead.
      if (!shouldRenderAsCluster(cluster)) {

        return;
      }

      Marker  marker  = mClusterMarkerCache.get(cluster);
      MarkerWithPosition markerWithPosition = null;
      if(null == marker){


      }else{
        markerWithPosition = new MarkerWithPosition(marker);
        onClusterUpdated(cluster, marker);

      }
      if(null != marker){
        onClusterRendered(cluster, marker);
        if(null != markerWithPosition){
          newMarkers.add(markerWithPosition);
        }


      }

    }
  }

  /**
   * A Marker and its position. Marker.getPosition() must be called from the UI thread, so this
   * object allows lookup from other threads.
   */
  private static class MarkerWithPosition {
    private final Marker marker;
    private LatLng position;

    private MarkerWithPosition(Marker marker) {
      this.marker = marker;
      position = marker.getPosition();
    }

    @Override
    public boolean equals(Object other) {
      if (other instanceof MarkerWithPosition) {
        return marker.equals(((MarkerWithPosition) other).marker);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return marker.hashCode();
    }
  }

  private static final TimeInterpolator ANIMATION_INTERP = new DecelerateInterpolator();

  /**
   * Animates a markerWithPosition from one position to another. TODO: improve performance for slow
   * devices (e.g. Nexus S).
   */
  @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
  private class AnimationTask extends AnimatorListenerAdapter
      implements ValueAnimator.AnimatorUpdateListener {
    private final MarkerWithPosition markerWithPosition;
    private final Marker marker;
    private final LatLng from;
    private final LatLng to;
    private boolean mRemoveOnComplete;
    private MarkerManager mMarkerManager;

    private AnimationTask(MarkerWithPosition markerWithPosition, LatLng from, LatLng to) {
      this.markerWithPosition = markerWithPosition;
      this.marker = markerWithPosition.marker;
      this.from = from;
      this.to = to;
    }

    public void perform() {
      ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, 1);
      valueAnimator.setInterpolator(ANIMATION_INTERP);
      valueAnimator.addUpdateListener(this);
      valueAnimator.addListener(this);
      valueAnimator.start();
    }

    @Override
    public void onAnimationEnd(Animator animation) {
      if (mRemoveOnComplete) {
        mMarkerCache.remove(marker);
        mClusterMarkerCache.remove(marker);
        mMarkerManager.remove(marker);
      }
      markerWithPosition.position = to;
    }

    public void removeOnAnimationComplete(MarkerManager markerManager) {
      mMarkerManager = markerManager;
      mRemoveOnComplete = true;
    }

    @Override
    public void onAnimationUpdate(ValueAnimator valueAnimator) {
      float fraction = valueAnimator.getAnimatedFraction();
      double lat = (to.latitude - from.latitude) * fraction + from.latitude;
      double lngDelta = to.longitude - from.longitude;

      // Take the shortest path across the 180th meridian.
      if (Math.abs(lngDelta) > 180) {
        lngDelta -= Math.signum(lngDelta) * 360;
      }
      double lng = lngDelta * fraction + from.longitude;
      LatLng position = new LatLng(lat, lng);
      marker.setPosition(position);
    }
  }
}
