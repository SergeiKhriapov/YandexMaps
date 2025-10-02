package ru.netology.nmedia.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.yandex.mapkit.MapKitFactory
import ru.netology.nmedia.R
import ru.netology.nmedia.model.MarkerPoint

class AppActivity : AppCompatActivity(R.layout.activity_app),
    PointsListFragment.OnMarkerSelectedListener {

    private var mapFragment: MapsFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Инициализация Yandex MapKit
        val apiKey = applicationContext.packageManager
            .getApplicationInfo(packageName, android.content.pm.PackageManager.GET_META_DATA)
            .metaData.getString("com.yandex.mapkit.API_KEY") ?: ""
        MapKitFactory.setApiKey(apiKey)
        MapKitFactory.initialize(this)

        if (savedInstanceState == null) {
            mapFragment = MapsFragment()
            supportFragmentManager.commit {
                replace(R.id.fragmentContainer, mapFragment!!)
            }
        } else {
            mapFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer) as? MapsFragment
        }
    }

    fun openFragment(fragment: Fragment) {
        supportFragmentManager.commit {
            replace(R.id.fragmentContainer, fragment)
            addToBackStack(null)
        }
    }

    override fun onMarkerSelected(marker: MarkerPoint) {
        // Возврат к карте
        supportFragmentManager.popBackStack()
        mapFragment?.moveCameraToMarker(marker)
    }
}
