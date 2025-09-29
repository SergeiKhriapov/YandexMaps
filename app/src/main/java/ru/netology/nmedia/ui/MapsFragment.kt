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
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.user_location.UserLocationLayer
import com.yandex.mapkit.user_location.UserLocationObjectListener
import com.yandex.mapkit.user_location.UserLocationView
import com.yandex.runtime.image.ImageProvider
import ru.netology.nmedia.R
import ru.netology.nmedia.model.MarkerPoint
import ru.netology.nmedia.storage.MarkerStorage
import toBitmap

class MapsFragment : Fragment(), UserLocationObjectListener {

    private lateinit var mapView: MapView
    private var userLocationLayer: UserLocationLayer? = null
    private var markers = mutableListOf<MarkerPoint>()
    private lateinit var markersLayer: MapObjectCollection

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

        // Плавное масштабирование через кнопки
        val zoomInButton: ImageButton = view.findViewById(R.id.zoomInButton)
        val zoomOutButton: ImageButton = view.findViewById(R.id.zoomOutButton)

        zoomInButton.setOnClickListener {
            val currentZoom = mapView.map.cameraPosition.zoom
            mapView.map.move(
                CameraPosition(mapView.map.cameraPosition.target, currentZoom + 1, 0f, 0f),
                Animation(Animation.Type.SMOOTH, 0.5f),
                null
            )
        }

        zoomOutButton.setOnClickListener {
            val currentZoom = mapView.map.cameraPosition.zoom
            mapView.map.move(
                CameraPosition(mapView.map.cameraPosition.target, currentZoom - 1, 0f, 0f),
                Animation(Animation.Type.SMOOTH, 0.5f),
                null
            )
        }

        // Проверка разрешений на геопозицию
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED -> enableUserLocation()
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) ->
                Toast.makeText(requireContext(), "Location permission needed", Toast.LENGTH_SHORT).show()
            else -> requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        // Загружаем маркеры
        markers = MarkerStorage.loadMarkers(requireContext())
        markersLayer = mapView.map.mapObjects.addCollection()
        refreshMarkersOnMap()

        // Слушатель кликов на карту
        mapView.map.addInputListener(object : com.yandex.mapkit.map.InputListener {
            override fun onMapTap(map: com.yandex.mapkit.map.Map, point: Point) {
                showAddMarkerDialog(point)
            }

            override fun onMapLongTap(map: com.yandex.mapkit.map.Map, point: Point) {}
        })

        // Камера на первый маркер или центр Москвы
        val initialPoint = markers.firstOrNull()
            ?.let { Point(it.latitude, it.longitude) }
            ?: Point(55.751999, 37.617734)

        mapView.post {
            mapView.map.move(
                CameraPosition(initialPoint, 15f, 0f, 0f),
                Animation(Animation.Type.SMOOTH, 1f),
                null
            )
        }

        // жесты
        mapView.map.isZoomGesturesEnabled = true
        mapView.map.isScrollGesturesEnabled = true
    }

    private fun showAddMarkerDialog(point: Point) {
        val editText = EditText(requireContext())
        AlertDialog.Builder(requireContext())
            .setTitle("Название точки")
            .setView(editText)
            .setPositiveButton("Добавить") { _, _ ->
                val name = editText.text.toString().trim()
                if (name.isNotEmpty()) addMarker(point, name)
                else Toast.makeText(requireContext(), "Название не может быть пустым", Toast.LENGTH_SHORT).show()
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
                    refreshMarkersOnMap()
                } else {
                    Toast.makeText(requireContext(), "Название не может быть пустым", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Удалить") { _, _ ->
                markers.remove(marker)
                MarkerStorage.saveMarkers(requireContext(), markers)
                refreshMarkersOnMap()
            }
            .setNeutralButton("Отмена", null)
            .show()
    }

    private fun addMarker(point: Point, name: String) {
        val id = System.currentTimeMillis()
        val marker = MarkerPoint(id, name, point.latitude, point.longitude)
        markers.add(marker)
        MarkerStorage.saveMarkers(requireContext(), markers)
        refreshMarkersOnMap()
    }

    private fun refreshMarkersOnMap() {
        markersLayer.clear()
        val inflater = LayoutInflater.from(requireContext())

        markers.forEach { marker ->
            val markerView = inflater.inflate(R.layout.item_marker, null)
            markerView.findViewById<TextView>(R.id.markerText).text = marker.name
            val icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_netology_48dp)
            markerView.findViewById<ImageView>(R.id.markerIcon).setImageDrawable(icon)

            val bitmap = markerView.toBitmap()
            val placemark = markersLayer.addPlacemark(Point(marker.latitude, marker.longitude))
            placemark.setIcon(ImageProvider.fromBitmap(bitmap))
            placemark.userData = marker
            placemark.addTapListener { _, _ ->
                showEditMarkerDialog(marker)
                true
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableUserLocation() {
        val mapKit = MapKitFactory.getInstance()
        userLocationLayer = mapKit.createUserLocationLayer(mapView.mapWindow)
        userLocationLayer?.isVisible = true
        userLocationLayer?.setHeadingModeActive(true)
        userLocationLayer?.setObjectListener(this)
    }

    override fun onObjectAdded(userLocationView: UserLocationView) {
        userLocationView.arrow.setIcon(
            ImageProvider.fromResource(requireContext(), R.drawable.ic_netology_48dp)
        )
        userLocationView.pin.setIcon(
            ImageProvider.fromResource(requireContext(), R.drawable.ic_netology_48dp)
        )
    }

    override fun onObjectRemoved(userLocationView: UserLocationView) {}
    override fun onObjectUpdated(userLocationView: UserLocationView, objectEvent: ObjectEvent) {}

    override fun onStart() {
        super.onStart()
        mapView.onStart()
        MapKitFactory.getInstance().onStart()
    }

    override fun onStop() {
        mapView.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }
}
