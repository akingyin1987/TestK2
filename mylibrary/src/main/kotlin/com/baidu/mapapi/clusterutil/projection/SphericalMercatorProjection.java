/*
 * Copyright (C) 2015 Baidu, Inc. All Rights Reserved.
 */
package com.baidu.mapapi.clusterutil.projection;

import com.baidu.mapapi.model.LatLng;

/**
 * 黑卡托投影坐标系
 * @author aking
 */
public class SphericalMercatorProjection {
  final double mWorldWidth;

  public SphericalMercatorProjection(final double worldWidth) {
    mWorldWidth = worldWidth;
  }


  public Point toPoint(final LatLng latLng) {
    final double x = latLng.longitude / 360 + .5;
    final double siny = Math.sin(Math.toRadians(latLng.latitude));
    final double y = 0.5 * Math.log((1 + siny) / (1 - siny)) / -(2 * Math.PI) + .5;

    return new Point(x * mWorldWidth, y * mWorldWidth);
  }

  public LatLng toLatLng(com.baidu.mapapi.clusterutil.projection.Point point) {
    final double x = point.x / mWorldWidth - 0.5;
    final double lng = x * 360;

    double y = .5 - (point.y / mWorldWidth);
    final double lat = 90 - Math.toDegrees(Math.atan(Math.exp(-y * 2 * Math.PI)) * 2);

    return new LatLng(lat, lng);
  }
}
