package ru.netology.nmedia.storage

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ru.netology.nmedia.model.MarkerPoint

object MarkerStorage {
    private const val PREFS_NAME = "markers_prefs"
    private const val KEY_MARKERS = "markers"

    fun saveMarkers(context: Context, markers: List<MarkerPoint>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = Gson().toJson(markers)
        prefs.edit().putString(KEY_MARKERS, json).apply()
    }

    fun loadMarkers(context: Context): MutableList<MarkerPoint> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_MARKERS, "[]")
        val type = object : TypeToken<List<MarkerPoint>>() {}.type
        return Gson().fromJson(json, type)
    }
}
