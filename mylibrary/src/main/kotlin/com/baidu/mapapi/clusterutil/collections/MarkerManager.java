/*
 * Copyright (C) 2015 Baidu, Inc. All Rights Reserved.
 */

package com.baidu.mapapi.clusterutil.collections;

import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;


/**
 * 所有的 Marker 操作（添加和删除）都应该通过它的集合类来进行。
 * 也就是说，你不能通过一个集合来添加一个 Marker，
 * 然后再通过 Marker.remove() 方法来删除它。
 * 所有的添加和删除操作都应该通过这个类来进行。
 * Keeps track of collections of markers on the map. Delegates all Marker-related events to each
 * collection's individually managed listeners.
 *
 * <p>All marker operations (adds and removes) should occur via its collection class. That is, don't
 * add a marker via a collection, then remove it via Marker.remove()
 * @author aking
 */
public class MarkerManager extends MapObjectManager<Marker,MarkerManager.Collection>
    implements BaiduMap.OnMarkerClickListener, BaiduMap.OnMarkerDragListener {


//  private final Map<String, Collection> mNamedCollections = new HashMap<String, Collection>();
//  private final Map<Marker, Collection> mAllMarkers = new HashMap<Marker, Collection>();

  public MarkerManager(BaiduMap map) {
    super(map);

  }

  @Override
  void setListenersOnUiThread() {
    if(null != mMap){
     // mMap.setOnMarkerDragListener(this);
     // mMap.setOnMarkerClickListener(this);
    }
  }

  public Collection newCollection() {
    return new Collection();
  }


  @Override
  public boolean onMarkerClick(Marker marker) {
    Collection collection = mAllObjects.get(marker);
    if (collection != null && collection.mMarkerClickListener != null) {
      // you can set the click action
      return collection.mMarkerClickListener.onMarkerClick(marker);
    } else {
      // click single maker out of cluster
    }
    return false;
  }

  @Override
  public void onMarkerDragStart(Marker marker) {
    Collection collection = mAllObjects.get(marker);
    if (collection != null && collection.mMarkerDragListener != null) {
      collection.mMarkerDragListener.onMarkerDragStart(marker);
    }
  }

  @Override
  public void onMarkerDrag(Marker marker) {
    Collection collection = mAllObjects.get(marker);
    if (collection != null && collection.mMarkerDragListener != null) {
      collection.mMarkerDragListener.onMarkerDrag(marker);
    }
  }

  @Override
  public void onMarkerDragEnd(Marker marker) {
    Collection collection = mAllObjects.get(marker);
    if (collection != null && collection.mMarkerDragListener != null) {

      collection.mMarkerDragListener.onMarkerDragEnd(marker);
    }
  }

  /**
   * Removes a marker from its collection.
   *
   * @param marker the marker to remove.
   * @return true if the marker was removed.
   */
  public boolean remove(Marker marker) {
   return super.remove(marker);
  }

  @Override
  protected void removeObjectFromMap(Marker object) {
       object.remove();
  }

  public class Collection extends MapObjectManager.Collection {
   // private final Set<Marker> mMarkers = new HashSet<Marker>();
    private BaiduMap.OnMarkerClickListener mMarkerClickListener;
    private BaiduMap.OnMarkerDragListener mMarkerDragListener;

    public Collection() {}

    public Marker addMarker(MarkerOptions opts) {

      Marker marker = (Marker) mMap.addOverlay(opts);
      super.add(marker);

      return marker;
    }

    public boolean remove(Marker marker) {
      return super.remove(marker);
    }



    /**
     * 这个可以得到一个集合的镜像， 它的返回结果不可直接被改变 即表示此集合只可读
     *
     * @return
     */
    public java.util.Collection<Marker> getMarkers() {
      return getObjects();
    }

    public void setOnMarkerClickListener(BaiduMap.OnMarkerClickListener markerClickListener) {
      mMarkerClickListener = markerClickListener;
    }

    public void setOnMarkerDragListener(BaiduMap.OnMarkerDragListener markerDragListener) {
      mMarkerDragListener = markerDragListener;
    }
  }
}
