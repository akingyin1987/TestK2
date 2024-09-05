package com.baidu.mapapi.overlayutil;

import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.OverlayOptions;
import java.util.ArrayList;
import java.util.List;

/**
 * @author king
 * @version V1.0 @ Description: @ Date 2019/12/31 17:17
 */
public abstract class BMapCacheOverlyManager extends OverlayManager {
  public BMapCacheOverlyManager(BaiduMap baiduMap) {
    super(baiduMap);
  }

  private List<OverlayOptions> mOverlayOptions = new ArrayList<>();

  @Override
  public List<OverlayOptions> getOverlayOptions() {
    return mOverlayOptions;
  }

  public void setOverlayOptions(List<OverlayOptions> overlayOptions) {
    mOverlayOptions = overlayOptions;
  }
}
