package com.ravi.busmanagementt.data.serivce

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.ravi.busmanagementt.R
import com.ravi.busmanagementt.domain.repository.UserRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.internal.notify
import javax.inject.Inject


@AndroidEntryPoint
class FCMService : FirebaseMessagingService() {


    @Inject
    lateinit var userRepository: UserRepository



    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        if (message.data.isNotEmpty()){
            val title = message.data["title"]
            val body = message.data["body"]
            if (!title.isNullOrEmpty() || !body.isNullOrEmpty()){
                sendNotification(title, body)
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCMService", "Refreshed token: $token")
        CoroutineScope(Dispatchers.IO).launch {
            userRepository.setFcmToken(token)
        }
    }



    private fun sendNotification(title: String?, body: String?){
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "bus_alerts_channel"

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val channel = NotificationChannel(channelId, "Bus Alert", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.bus_marker_icon)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)


        notificationManager.notify(0, notificationBuilder.build())

    }

}