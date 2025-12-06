package com.ravi.busmanagementt.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import androidx.core.graphics.createBitmap

fun Int.dpToInt(context: Context): Int = (this * context.resources.displayMetrics.density).toInt()
fun bitmapDescriptor(
    context: Context,
    icon: Int,
    widthInDp: Int = 48,
    heightInDp: Int = 48
) : BitmapDescriptor? {
    return try {
        val drawable = ContextCompat.getDrawable(context,icon) ?: return null
        val width = widthInDp.dpToInt(context)
        val height = heightInDp.dpToInt(context)
        drawable.setBounds(0,0, width, height)
        val bitmap = createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.draw(canvas)
         BitmapDescriptorFactory.fromBitmap(bitmap)
    } catch (e: Exception) {
        Log.d("bitmapDescriptor", "Exception: bitmapDescriptor: ${e.message}")
        null
    }
}




