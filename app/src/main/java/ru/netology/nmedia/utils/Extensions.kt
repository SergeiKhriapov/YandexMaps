package ru.netology.nmedia.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.core.graphics.createBitmap

fun Drawable.toBitmap(): Bitmap {
    val bitmap = createBitmap(
        intrinsicWidth.takeIf { it > 0 } ?: 48,
        intrinsicHeight.takeIf { it > 0 } ?: 48
    )
    val canvas = Canvas(bitmap)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bitmap
}
