package ru.netology.nework.map

import android.content.Context
import android.graphics.PointF
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.IconStyle
import com.yandex.mapkit.map.InputListener
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.user_location.UserLocationLayer
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.FragmentScoped
import ru.netology.nework.R
import ru.netology.nework.util.DrawableImageProvider
import ru.netology.nework.util.ImageInfo
import java.lang.ref.WeakReference
import javax.inject.Inject

@FragmentScoped
class YandexMapManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) : DefaultLifecycleObserver {
    private var currentMapView: MapView? = null
    private var mapInputListener: InputListener? = null
    private var userLocationLayer: UserLocationLayer? = null
    private var currentPlacemark: PlacemarkMapObject? = null
    private val imageProvider = DrawableImageProvider(
        context,
        ImageInfo(R.drawable.ic_location_24)
    )
    private val iconStyle = IconStyle().apply { anchor = PointF(0.5f, 1.0f) }

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
        currentPlacemark = map.mapObjects.addPlacemark { placemarkMapObject ->
            placemarkMapObject.geometry = targetPoint
            placemarkMapObject.setIcon(imageProvider)
            placemarkMapObject.setIconStyle(iconStyle)
        }
        map.move(CameraPosition(targetPoint, 15.0f, 0.0f, 0.0f))
    }

    fun setupLocationPicker(onPointSelected: (Double, Double) -> Unit) {
        val mapView = currentMapView ?: return
        val map = mapView.mapWindow.map
        if (mapInputListener != null) {
            map.removeInputListener(WeakReference(mapInputListener))
        }
        mapInputListener = object : InputListener {
            override fun onMapTap(map: Map, point: Point) {
                map.mapObjects.clear()
                currentPlacemark = map.mapObjects.addPlacemark { placemarkMapObject ->
                    placemarkMapObject.geometry = point
                    placemarkMapObject.setIcon(imageProvider)
                    placemarkMapObject.setIconStyle(iconStyle)
                    onPointSelected(point.latitude, point.longitude)
                }
            }

            override fun onMapLongTap(map: Map, point: Point) {}
        }

        map.addInputListener(WeakReference(mapInputListener))
    }

    fun moveToUserLocation() {
        val mapView = currentMapView ?: return
        userLocationLayer?.cameraPosition()?.target?.let { target ->
            mapView.mapWindow.map.move(
                CameraPosition(target, 16.0f, 0.0f, 0.0f),
                Animation(Animation.Type.SMOOTH, 0.5f)
            )
        }
    }

    fun changeZoom(step: Float) {
        val mapView = currentMapView ?: return
        val map = mapView.mapWindow.map
        val currentPosition = map.cameraPosition
        val newPosition = CameraPosition(
            currentPosition.target,
            currentPosition.zoom + step,
            currentPosition.azimuth,
            currentPosition.tilt
        )
        map.move(
            newPosition,
            Animation(Animation.Type.SMOOTH, 0.2F),
            null
        )
    }

    fun disableAllGestures() {
        val mapView = currentMapView ?: return
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
        if (mapInputListener != null) {
            currentMapView?.mapWindow?.map?.removeInputListener(WeakReference(mapInputListener))
        }
        userLocationLayer = null
        mapInputListener = null
        currentMapView = null
        currentPlacemark = null
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