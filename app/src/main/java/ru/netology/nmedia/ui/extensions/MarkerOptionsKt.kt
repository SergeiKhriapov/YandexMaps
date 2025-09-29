package ru.netology.nmedia.ui.extensions

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.runtime.image.ImageProvider

fun PlacemarkMapObject.setIcon(drawable: Drawable) {
    // Создаём bitmap нужного размера
    val bitmap = Bitmap.createBitmap(
        drawable.intrinsicWidth,
        drawable.intrinsicHeight,
        Bitmap.Config.ARGB_8888
    )

    // Создаём canvas с этим bitmap
    val canvas = Canvas(bitmap)

    // Устанавливаем размеры drawable и рисуем его на canvas
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)

    // Устанавливаем иконку маркера через ImageProvider
    this.setIcon(ImageProvider.fromBitmap(bitmap))
}
