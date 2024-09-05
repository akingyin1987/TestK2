package com.baidu.mapapi.overlayutil

import android.text.TextUtils
import com.baidu.mapapi.map.BaiduMap
import com.baidu.mapapi.map.BaiduMap.OnMarkerClickListener
import com.baidu.mapapi.map.BaiduMap.OnPolylineClickListener
import com.baidu.mapapi.map.CircleOptions
import com.baidu.mapapi.map.MapStatusUpdateFactory
import com.baidu.mapapi.map.Marker
import com.baidu.mapapi.map.MarkerOptions
import com.baidu.mapapi.map.Overlay
import com.baidu.mapapi.map.OverlayOptions
import com.baidu.mapapi.map.PolygonOptions
import com.baidu.mapapi.model.LatLngBounds

import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * 该类提供一个能够显示和管理多个Overlay的基类
 *
 *
 * 复写[.getOverlayOptions] 设置欲显示和管理的Overlay列表
 *
 *
 * 通过 [ ][com.baidu.mapapi.map.BaiduMap.setOnMarkerClickListener]
 * 将覆盖物点击事件传递给OverlayManager后，OverlayManager才能响应点击事件。
 *
 *
 * 复写[.onMarkerClick] 处理Marker点击事件
 */
abstract class OverlayManager(var baiduMap: BaiduMap) : OnMarkerClickListener,
    OnPolylineClickListener {
    private val mLock: ReadWriteLock = ReentrantReadWriteLock()



    private var mOverlayOptionList = mutableListOf<OverlayOptions>()

    @JvmField
    var mOverlayList= mutableListOf<Overlay>()



    private fun lock() {
        mLock.writeLock().lock()
    }

    private fun unlock() {
        mLock.writeLock().unlock()
    }

    /**
     * 覆写此方法设置要管理的Overlay列表
     *
     * @return 管理的Overlay列表
     */
    abstract val overlayOptions: List<OverlayOptions>


    fun getMarker(uuid: String): Marker? {
        try {
            lock()

            if (TextUtils.isEmpty(uuid)) {
                return null
            }
            for (overlay in mOverlayList) {
                val bundle = overlay.extraInfo
//                if (overlay is Marker && null != bundle && bundle.containsKey(BDMapManager.BAIDU_MARKER_UUID)) {
//                    if (TextUtils.equals(uuid, bundle.getString(BDMapManager.BAIDU_MARKER_UUID))) {
//                        return overlay
//                    }
//                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            unlock()
        }

        return null
    }

    fun getMarker(index: Int): Marker? {
        for (overlay in mOverlayList) {
            val bundle = overlay.extraInfo
            if (overlay is Marker && null != bundle && bundle.containsKey("index")) {
                if (index == bundle.getInt("index")) {
                    return overlay
                }
            }
        }
        return null
    }

    fun getOverLay(index: Int): Overlay? {
        for (overlay in mOverlayList) {
            val bundle = overlay.extraInfo
            if (null != bundle && bundle.containsKey("index")) {
                if (index == bundle.getInt("index")) {
                    return overlay
                }
            }
        }
        return null
    }

    fun addToMap(overlayOptions: MarkerOptions): Marker {


        return baiduMap.addOverlay(overlayOptions) as Marker
    }

    /** 将所有Overlay 添加到地图上  */
    @Synchronized
    fun addToMap() {
        try {
            lock()

            removeFromMap()

            val overlayOptions = overlayOptions
            if (overlayOptions != null) {
                mOverlayOptionList.addAll(overlayOptions)
                // mOverlayOptionList.addAll(getOverlayOptions());
            }

            for (option in mOverlayOptionList) {
                mOverlayList.add(baiduMap.addOverlay(option))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            unlock()
        }
    }

    /** 将所有Overlay 从 地图上消除  */
    fun removeFromMap() {

        for (marker in mOverlayList) {
            marker.remove()
        }
        mOverlayOptionList.clear()
        mOverlayList.clear()
    }

    /**
     * 缩放地图，使所有Overlay都在合适的视野内
     *
     *
     * 注： 该方法只对Marker类型的overlay有效
     */
    fun zoomToSpan() {


        if (mOverlayList.size > 0) {
            val builder = LatLngBounds.Builder()
            for (overlay in mOverlayList) {
                // polyline 中的点可能太多，只按marker 缩放
                if (overlay is Marker) {
                    builder.include(overlay.position)
                }
            }
            baiduMap.setMapStatus(MapStatusUpdateFactory.newLatLngBounds(builder.build()))
        }
    }

    fun zoomToSpanAll() {

        val builder = LatLngBounds.Builder()
        if (mOverlayList.size > 0) {
            for (overlay in mOverlayList) {
                // polyline 中的点可能太多，只按marker 缩放
                if (overlay is Marker) {
                    builder.include(overlay.position)
                }
            }
        }
        if (mOverlayOptionList.size > 0) {
            for (overlayOptions in mOverlayOptionList) {
                if (overlayOptions is CircleOptions) {
                    builder.include(overlayOptions.center)
                }

                if (overlayOptions is PolygonOptions) {
                    val latLngs = overlayOptions.points
                    if (null != latLngs && latLngs.size > 0) {
                        for (latLng in latLngs) {
                            builder.include(latLng)
                        }
                    }
                }
            }
        }
        val latLngBounds = builder.build()
        if (null != latLngBounds.northeast && null != latLngBounds.southwest) {

            if (latLngBounds.northeast.latitude != 0.0 && latLngBounds.northeast.longitude != 0.0) {
                baiduMap.setMapStatus(MapStatusUpdateFactory.newLatLngBounds(builder.build()))
            }
        }
    }
}
