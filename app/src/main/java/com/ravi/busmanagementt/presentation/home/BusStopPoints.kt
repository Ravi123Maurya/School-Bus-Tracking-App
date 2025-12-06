package com.ravi.busmanagementt.presentation.home

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import com.ravi.busmanagementt.data.repository.RealtimeLocation
import com.ravi.busmanagementt.ui.theme.AppColors
import com.ravi.busmanagementt.utils.DistanceMatrix
import com.ravi.busmanagementt.utils.TimeMatrix
import java.util.Date

// todo : remove dummy data
// todo : fetch real stop points, calculate distance and time matrix in realtime, update status persistently,
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun BusStopPoints(
    modifier: Modifier = Modifier,
    stops: List<com.ravi.busmanagementt.domain.model.BusStop> = emptyList(),
    realtimeLocation: List<RealtimeLocation>?
) {

    val stopList = remember(realtimeLocation, stops) {

        if (realtimeLocation.isNullOrEmpty()) {
            return@remember stops.mapIndexed { i, stop ->
                BusStop(
                    id = i + 1,
                    name = stop.stopName,
                    scheduledTime = "",
                    status = StopStatus.PENDING
                )
            }
        }



        val lastLocationUpdate = realtimeLocation.last()
        val stopsReachedCount = lastLocationUpdate.numberOfStopsReached
        var reachedTime = ""
        stops.mapIndexed { i, stop ->
            var calculatedEta: Int? = null
            val status = when {
                i < stopsReachedCount-> {
                    reachedTime = TimeMatrix.formatTimestampToReadableTime(lastLocationUpdate.timestamp.toLong(), "h:mm a")
                    StopStatus.REACHED
                }
                i == stopsReachedCount -> StopStatus.CURRENT
                else -> StopStatus.PENDING
            }

            if (status != StopStatus.REACHED){
                calculatedEta = DistanceMatrix.calculateScheduleTimeETA(
                    realtimeLocations = realtimeLocation,
                    stopLocation = LatLng(stop.geoPoint.latitude, stop.geoPoint.longitude)
                )
            }

            BusStop(
                id = i + 1,
                name = stop.stopName,
                scheduledTime = calculatedEta.toString(),
                actualTime = reachedTime,
                status = status
            )
        }
    }



    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Pickup/Stop Points",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(Modifier.height(24.dp))

        stopList.forEachIndexed { index, stop ->
            StopPoint(
                stop = stop,
                isLast = index == stopList.lastIndex
            )
        }
    }
}

@Composable
fun StopPoint(
    modifier: Modifier = Modifier,
    stop: BusStop,
    isLast: Boolean = false
) {
    Row(modifier = modifier.fillMaxWidth()) {
        // Timeline indicator column
        StopTimeline(
            status = stop.status,
            isLast = isLast
        )

        Spacer(Modifier.width(16.dp))

        // Stop information column
        StopInfo(stop = stop)
    }
}

@Composable
private fun StopTimeline(
    status: StopStatus,
    isLast: Boolean
) {
    val circleColor = when (status) {
        StopStatus.REACHED -> AppColors.PurpleBlue
        StopStatus.CURRENT -> AppColors.PurpleBlue
        StopStatus.PENDING -> Color.LightGray
    }
    val dotsColor = when (status) {
        StopStatus.REACHED -> AppColors.PurpleBlue
        StopStatus.CURRENT -> Color.LightGray
        StopStatus.PENDING -> Color.LightGray
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Circle indicator
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(circleColor)
                .border(
                    width = if (status == StopStatus.CURRENT) 3.dp else 0.dp,
                    color = if (status == StopStatus.CURRENT) AppColors.PurpleBlue.copy(alpha = 0.3f) else Color.Transparent,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            when (status) {
                StopStatus.REACHED -> Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Completed",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )

                StopStatus.CURRENT -> Box(
                    Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )

                StopStatus.PENDING -> Box(
                    Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
        }

        // Connecting dots
        if (!isLast) {
            Spacer(Modifier.height(4.dp))
            repeat(12) {
                Box(
                    Modifier
                        .size(3.dp, 5.dp)
                        .clip(CircleShape)
                        .background(dotsColor)
                )
                Spacer(Modifier.height(3.dp))
            }
        }
    }
}

@Composable
private fun StopInfo(stop: BusStop) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp)
    ) {
        // Header row with stop name and number
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stop.name,
                color = if (stop.status == StopStatus.PENDING) Color.Gray else Color.Black,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )

            StopBadge(
                stopNumber = stop.id,
                status = stop.status
            )
        }

        Spacer(Modifier.height(8.dp))

        // Status and time information
        StopStatusText(stop = stop) // todo (Pick/Stop Points) : calculate ETA and remaining distance
    }
}

@Composable
private fun StopBadge(
    stopNumber: Int,
    status: StopStatus
) {
    val backgroundColor = when (status) {
        StopStatus.REACHED -> Color(0xFF4CAF50)
        StopStatus.CURRENT -> AppColors.PurpleBlue
        StopStatus.PENDING -> Color.LightGray
    }

    Text(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        text = "Stop $stopNumber",
        fontSize = 12.sp,
        color = Color.White,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun StopStatusText(stop: BusStop) {
    val (statusText, statusColor) = when (stop.status) {
        StopStatus.REACHED -> {
            "Reached at ${stop.actualTime}" to Color(0xFF4CAF50)
        }

        StopStatus.CURRENT -> {
            "Arriving now" to AppColors.PurpleBlue
        }

        StopStatus.PENDING -> {
            "Not reached yet" to Color.Gray
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(statusColor)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = statusText,
            fontSize = 13.sp,
            color = statusColor,
            fontWeight = FontWeight.Medium
        )
    }
}

// Data model for better type safety and clarity
data class BusStop(
    val id: Int,
    val name: String,
    val scheduledTime: String,
    val actualTime: String? = null,
    val status: StopStatus = StopStatus.PENDING
)

enum class StopStatus {
    REACHED,
    CURRENT,
    PENDING
}