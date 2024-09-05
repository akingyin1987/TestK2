package com.akingyin.mylibrary2

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.UiComposable
import androidx.compose.ui.platform.LocalContext
import com.baidu.mapapi.clusterutil.clustering.Cluster
import com.baidu.mapapi.clusterutil.clustering.ClusterItem
import com.baidu.mapapi.clusterutil.clustering.ClusterManager
import com.baidu.mapapi.clusterutil.clustering.view.ClusterRenderer





/**
 * Groups many items on a map based on zoom level.
 *
 * @param items all items to show
 * @param onClusterClick a lambda invoked when the user clicks a cluster of items
 * @param onClusterItemClick a lambda invoked when the user clicks a non-clustered item
 * @param onClusterItemInfoWindowClick a lambda invoked when the user clicks the info window of a
 * non-clustered item
 * @param onClusterItemInfoWindowLongClick a lambda invoked when the user long-clicks the info
 * window of a non-clustered item
 * @param clusterContent an optional Composable that is rendered for each [Cluster].
 * @param clusterItemContent an optional Composable that is rendered for each non-clustered item.
 */
@Composable
public fun <T : ClusterItem> Clustering(
    items: Collection<T>,
    onClusterClick: (Cluster<T>) -> Boolean = { false },
    onClusterItemClick: (T) -> Boolean = { false },
    onClusterItemInfoWindowClick: (T) -> Unit = { },
    onClusterItemInfoWindowLongClick: (T) -> Unit = { },
    clusterContent: @[UiComposable Composable] ((Cluster<T>) -> Unit)? = null,
    clusterItemContent: @[UiComposable Composable] ((T) -> Unit)? = null,
) {
    val clusterManager = rememberClusterManager<T>()
    val renderer = rememberClusterRenderer(clusterContent, clusterItemContent, clusterManager)
    SideEffect {
        if (clusterManager?.renderer != renderer) {
            clusterManager?.renderer = renderer ?: return@SideEffect
        }
    }

    SideEffect {
        clusterManager ?: return@SideEffect
        clusterManager.setOnClusterClickListener(onClusterClick)
        clusterManager.setOnClusterItemClickListener(onClusterItemClick)
        clusterManager.setOnClusterItemInfoWindowClickListener(onClusterItemInfoWindowClick)
       // clusterManager.setOnClusterItemInfoWindowLongClickListener(onClusterItemInfoWindowLongClick)
    }

    if (clusterManager != null) {
        Clustering(
            items = items,
            clusterManager = clusterManager,
        )
    }
}

/**
 * Groups many items on a map based on clusterManager.
 *
 * @param items all items to show
 * @param clusterManager a [ClusterManager] that can be used to specify the algorithm used by the rendering.
 */
@Composable
public fun <T : ClusterItem> Clustering(
    items: Collection<T>,
    clusterManager: ClusterManager<T>,
) {
    ResetMapListeners(clusterManager)

   // val cameraPositionState = currentCameraPositionState
//    LaunchedEffect(cameraPositionState) {
//        snapshotFlow { cameraPositionState.isMoving }
//            .collect { isMoving ->
//                if (!isMoving) {
//                    clusterManager.onMapStatusChangeFinish(cameraPositionState.position)
//                }
//            }
//    }
    val itemsState = rememberUpdatedState(items)
    LaunchedEffect(itemsState) {
        snapshotFlow { itemsState.value.toList() }
            .collect { items ->
                clusterManager.clearItems()
                clusterManager.addItems(items)
                clusterManager.cluster()
            }
    }
    DisposableEffect(itemsState) {
        onDispose {
            clusterManager.clearItems()
            clusterManager.cluster()
        }
    }
}




/**
 * Default Renderer for drawing Composable.
 *
 * @param clusterContent an optional Composable that is rendered for each [Cluster].
 * @param clusterItemContent an optional Composable that is rendered for each non-clustered item.
 */
@Composable
public fun <T : ClusterItem> rememberClusterRenderer(
    clusterContent: @Composable ((Cluster<T>) -> Unit)?,
    clusterItemContent: @Composable ((T) -> Unit)?,
    clusterManager: ClusterManager<T>?,
): ClusterRenderer<T>? {
    val clusterContentState = rememberUpdatedState(clusterContent)
    val clusterItemContentState = rememberUpdatedState(clusterItemContent)
    val context = LocalContext.current

    val clusterRendererState: MutableState<ClusterRenderer<T>?> = remember { mutableStateOf(null) }

    clusterManager ?: return null

    return clusterRendererState.value
}

@Composable
public fun <T : ClusterItem> rememberClusterManager(): ClusterManager<T>? {
    val context = LocalContext.current
    val clusterManagerState: MutableState<ClusterManager<T>?> = remember { mutableStateOf(null) }

    return clusterManagerState.value
}





@Composable
private fun ResetMapListeners(
    clusterManager: ClusterManager<*>,
) {

    LaunchedEffect(clusterManager) {
        Handler(Looper.getMainLooper()).post {

        }
    }
}
