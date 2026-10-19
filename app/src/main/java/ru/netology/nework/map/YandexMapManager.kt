package ru.netology.nework.map

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.InputListener
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.user_location.UserLocationLayer
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityScoped
import ru.netology.nework.R
import ru.netology.nework.util.DrawableImageProvider
import ru.netology.nework.util.ImageInfo
import javax.inject.Inject

@ActivityScoped
class YandexMapManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) : DefaultLifecycleObserver {
    private var currentMapView: MapView? = null
    private var mapInputListener: InputListener? = null
    private var userLocationLayer: UserLocationLayer? = null
    private val imageProvider = DrawableImageProvider(
        context,
        ImageInfo(R.drawable.ic_location_24)
    )

    init {
        MapKitFactory.initialize(context)
    }

    fun bindMapView(mapView: MapView) {
        detachMap()
        currentMapView = mapView
    }

    fun showPoint(latitude: Double, longitude: Double) {
        val mapView = currentMapView ?: return
        val map = mapView.mapWindow.map
        val targetPoint = Point(latitude, longitude)
        map.mapObjects.clear()
        map.mapObjects.addPlacemark { placemarkMapObject ->
            placemarkMapObject.geometry = targetPoint
            placemarkMapObject.setIcon(imageProvider)
        }
        map.move(CameraPosition(targetPoint, 15.0f, 0.0f, 0.0f))
    }

    fun disableAllGestures(mapView: MapView) {
        val map = mapView.mapWindow.map
        map.isScrollGesturesEnabled = false
        map.isZoomGesturesEnabled = false
        map.isTiltGesturesEnabled = false
        map.isRotateGesturesEnabled = false
    }

    fun enableUserLocation() {
        val mapView = currentMapView ?: return
        if (userLocationLayer == null) {
            userLocationLayer = MapKitFactory.getInstance()
                .createUserLocationLayer(mapView.mapWindow)
                .apply {
                    isVisible = true
                    isHeadingModeActive = true
                }
        }
    }

    fun detachMap() {
        mapInputListener = null
        currentMapView = null
    }

    override fun onStart(owner: LifecycleOwner) {
        MapKitFactory.getInstance().onStart()
        currentMapView?.onStart()
    }

    override fun onStop(owner: LifecycleOwner) {
        currentMapView?.onStop()
        MapKitFactory.getInstance().onStop()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        detachMap()
    }
}