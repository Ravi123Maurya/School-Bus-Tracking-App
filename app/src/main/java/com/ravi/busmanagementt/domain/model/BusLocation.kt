package com.ravi.busmanagementt.domain.model

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat.getSystemService
import com.google.android.gms.maps.model.LatLng
import com.ravi.busmanagementt.R


data class BusLocation(
    val busId: String,
    val liveLocationPoints: List<LatLng>
)


@RequiresApi(Build.VERSION_CODES.O)
fun ss(context: Context): Notification{
    val id = "idd"
    val chnel = NotificationChannel(id, "noti", NotificationManager.IMPORTANCE_HIGH)
    getSystemService(context, NotificationManager::class.java)?.createNotificationChannel(chnel)
    return NotificationCompat.Builder(context, id)
        .setSmallIcon(R.drawable.ic_launcher_background)
        .build()

}