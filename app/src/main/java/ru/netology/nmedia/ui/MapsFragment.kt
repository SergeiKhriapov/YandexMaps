package ru.netology.nmedia.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.layers.ObjectEvent
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.InputListener
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.user_location.UserLocationLayer
import com.yandex.mapkit.user_location.UserLocationObjectListener
import com.yandex.mapkit.user_location.UserLocationView
import com.yandex.runtime.image.ImageProvider
import ru.netology.nmedia.R
import ru.netology.nmedia.model.MarkerPoint
import ru.netology.nmedia.storage.MarkerStorage

class MapsFragment : Fragment(), UserLocationObjectListener {

    private lateinit var mapView: MapView
    private var userLocationLayer: UserLocationLayer? = null

    private var markers = mutableListOf<MarkerPoint>()
    private lateinit var markersLayer: MapObjectCollection

    private val placemarks = mutableMapOf<Long, PlacemarkMapObject>()

    private val mapInputListener = object : InputListener {
        override fun onMapTap(map: Map, point: Point) {
            showAddMarkerDialog(point)
        }

        override fun onMapLongTap(map: Map, point: Point) {
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) enableUserLocation()
            else Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show()
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_maps, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mapView = view.findViewById(R.id.mapView)

        val zoomInButton: ImageButton = view.findViewById(R.id.zoomInButton)
        val zoomOutButton: ImageButton = view.findViewById(R.id.zoomOutButton)
        val myLocationButton: ImageButton = view.findViewById(R.id.myLocationButton)

        zoomInButton.setOnClickListener { changeZoom(1f) }
        zoomOutButton.setOnClickListener { changeZoom(-1f) }

        myLocationButton.setOnClickListener {
            userLocationLayer?.cameraPosition()?.target?.let { location ->
                mapView.map.move(
                    CameraPosition(location, 15f, 0f, 0f),
                    Animation(Animation.Type.SMOOTH, 1f),
                    null
                )
            } ?: Toast.makeText(requireContext(), "User location not available", Toast.LENGTH_SHORT).show()
        }

        setupMarkers()

        enableGestures()
        moveCameraToInitialPoint()

        checkLocationPermission()
    }

    private fun changeZoom(delta: Float) {
        val currentZoom = mapView.map.cameraPosition.zoom
        mapView.map.move(
            CameraPosition(mapView.map.cameraPosition.target, currentZoom + delta, 0f, 0f),
            Animation(Animation.Type.SMOOTH, 0.5f),
            null
        )
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED -> enableUserLocation()
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) ->
                Toast.makeText(requireContext(), "Location permission needed", Toast.LENGTH_SHORT).show()
            else -> requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun setupMarkers() {
        markers.clear()
        markers.addAll(MarkerStorage.loadMarkers(requireContext()))

        markersLayer = mapView.map.mapObjects.addCollection()

        markers.forEach { addPlacemarkToMap(it) }
    }

    private fun enableGestures() {
        mapView.map.isZoomGesturesEnabled = true
        mapView.map.isScrollGesturesEnabled = true
    }

    private fun moveCameraToInitialPoint() {
        val initialPoint = markers.firstOrNull()?.let { Point(it.latitude, it.longitude) }
            ?: Point(55.751999, 37.617734)
        mapView.post {
            mapView.map.move(
                CameraPosition(initialPoint, 15f, 0f, 0f),
                Animation(Animation.Type.SMOOTH, 1f),
                null
            )
        }
    }

    private fun showAddMarkerDialog(point: Point) {
        val editText = EditText(requireContext())
        AlertDialog.Builder(requireContext())
            .setTitle("Название точки")
            .setView(editText)
            .setPositiveButton("Добавить") { _, _ ->
                val name = editText.text.toString().trim()
                if (name.isNotEmpty()) {
                    addMarker(point, name)
                } else {
                    Toast.makeText(requireContext(), "Название не может быть пустым", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showEditMarkerDialog(marker: MarkerPoint) {
        val editText = EditText(requireContext())
        editText.setText(marker.name)
        AlertDialog.Builder(requireContext())
            .setTitle("Редактировать точку")
            .setView(editText)
            .setPositiveButton("Сохранить") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    marker.name = newName
                    MarkerStorage.saveMarkers(requireContext(), markers)
                    refreshMarker(marker)
                } else {
                    Toast.makeText(requireContext(), "Название не может быть пустым", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Удалить") { _, _ ->
                // удаляем маркер и его placemark
                markers.remove(marker)
                MarkerStorage.saveMarkers(requireContext(), markers)
                removeMarkerFromMap(marker)
            }
            .setNeutralButton("Отмена", null)
            .show()
    }

    private fun addMarker(point: Point, name: String) {
        val marker = MarkerPoint(System.currentTimeMillis(), name, point.latitude, point.longitude)
        markers.add(marker)
        MarkerStorage.saveMarkers(requireContext(), markers)
        addPlacemarkToMap(marker)
    }

    private fun addPlacemarkToMap(marker: MarkerPoint) {
        val inflater = LayoutInflater.from(requireContext())
        val markerView = inflater.inflate(R.layout.item_marker, null)
        markerView.findViewById<TextView>(R.id.markerText).text = marker.name
        markerView.findViewById<ImageView>(R.id.markerIcon)
            .setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_netology_48dp))

        val bitmap = markerViewToBitmap(markerView)
        val placemark = markersLayer.addPlacemark(Point(marker.latitude, marker.longitude))
        placemark.setIcon(ImageProvider.fromBitmap(bitmap))
        placemark.userData = marker.id
        placemark.zIndex = 1f
        placemark.addTapListener { _, _ ->
            showEditMarkerDialog(marker)
            true
        }

        // сохраняем ссылку
        placemarks[marker.id] = placemark
    }

    private fun refreshMarker(marker: MarkerPoint) {
        val placemark = placemarks[marker.id] ?: return
        val inflater = LayoutInflater.from(requireContext())
        val markerView = inflater.inflate(R.layout.item_marker, null)
        markerView.findViewById<TextView>(R.id.markerText).text = marker.name
        markerView.findViewById<ImageView>(R.id.markerIcon)
            .setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_netology_48dp))
        placemark.setIcon(ImageProvider.fromBitmap(markerViewToBitmap(markerView)))
    }

    private fun removeMarkerFromMap(marker: MarkerPoint) {
        val placemark = placemarks.remove(marker.id)
        if (placemark != null) {
            markersLayer.remove(placemark)
        }
    }

    private fun markerViewToBitmap(view: View): android.graphics.Bitmap {
        view.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        val bitmap = android.graphics.Bitmap.createBitmap(
            view.measuredWidth.coerceAtLeast(1),
            view.measuredHeight.coerceAtLeast(1),
            android.graphics.Bitmap.Config.ARGB_8888
        )
        val canvas = android.graphics.Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    @SuppressLint("MissingPermission")
    private fun enableUserLocation() {
        val mapKit = MapKitFactory.getInstance()
        if (userLocationLayer == null) {
            userLocationLayer = mapKit.createUserLocationLayer(mapView.mapWindow)
            userLocationLayer?.isVisible = true
            userLocationLayer?.setHeadingModeActive(true)
            userLocationLayer?.setObjectListener(this)
        }
    }

    override fun onObjectAdded(userLocationView: UserLocationView) {
        userLocationView.arrow.setIcon(ImageProvider.fromResource(requireContext(), R.drawable.ic_netology_48dp))
        userLocationView.pin.setIcon(ImageProvider.fromResource(requireContext(), R.drawable.ic_netology_48dp))
    }

    override fun onObjectRemoved(userLocationView: UserLocationView) {}
    override fun onObjectUpdated(userLocationView: UserLocationView, objectEvent: ObjectEvent) {}

    override fun onStart() {
        super.onStart()
        mapView.onStart()
        MapKitFactory.getInstance().onStart()
        mapView.map.addInputListener(mapInputListener)
    }

    override fun onStop() {
        mapView.map.removeInputListener(mapInputListener)

        mapView.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }
}
