/*
 * Copyright (C) 2015 Baidu, Inc. All Rights Reserved.
 */
package com.baidu.mapapi.clusterutil.clustering

import com.baidu.mapapi.map.BitmapDescriptor
import com.baidu.mapapi.model.LatLng

/**
 * https://youtrack.jetbrains.com/issue/KT-49404/Fix-type-unsoundness-for-contravariant-captured-type-based-on-Java-class
 * 每个Marker点，包含Marker点坐标以及图标
 * @author aking
 */
interface ClusterItem {
    /**
     * 返回 定位坐标信息
     * @return LatLng
     */

    val position: LatLng

    /**
     * 返回定位 点的图标
     * @return BitmapDescriptor
     */

    val bitmapDescriptor: BitmapDescriptor?
}
