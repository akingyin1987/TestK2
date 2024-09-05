/*
 * Copyright (C) 2015 Baidu, Inc. All Rights Reserved.
 */
package com.baidu.mapapi.clusterutil.clustering

import com.baidu.mapapi.model.LatLng

/** A collection of ClusterItems that are nearby each other.
 * @author aking
 */
interface Cluster<T : ClusterItem> {

    val position: LatLng?


    val items: Collection<T>


    val size: Int
}
