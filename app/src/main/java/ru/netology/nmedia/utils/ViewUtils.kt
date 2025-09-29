import android.graphics.Bitmap
import android.view.View
import androidx.core.graphics.createBitmap

fun View.toBitmap(): Bitmap {
    measure(
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
    )
    layout(0, 0, measuredWidth, measuredHeight)
    val bitmap = createBitmap(measuredWidth, measuredHeight)
    val canvas = android.graphics.Canvas(bitmap)
    draw(canvas)
    return bitmap
}
