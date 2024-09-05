package com.baidu.mapapi.clusterutil.clustering.algo;

import com.baidu.mapapi.clusterutil.clustering.ClusterItem;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author: aking @CreateDate: 2022/4/29 10:12 @UpdateUser: 更新者 @UpdateDate: 2022/4/29
 *     10:12 @UpdateRemark: 更新说明 @Version: 1.0
 */
public abstract class AbstractAlgorithm<T extends ClusterItem> implements Algorithm<T> {

  private final ReadWriteLock mLock = new ReentrantReadWriteLock();

  @Override
  public void lock() {
    this.mLock.writeLock().lock();
  }

  @Override
  public void unlock() {
    this.mLock.writeLock().unlock();
  }
}
