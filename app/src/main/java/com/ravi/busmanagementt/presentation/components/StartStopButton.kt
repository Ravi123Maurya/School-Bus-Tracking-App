package com.ravi.busmanagementt.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ravi.busmanagementt.data.serivce.LocationSharingState


@Preview
@Composable
fun StartStopButton(
    isLocationPointsEmpty: Boolean = false,
    sharingState: LocationSharingState = LocationSharingState.IDLE,
    onClick: () -> Unit = {}
) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (sharingState == LocationSharingState.SHARING) Color.Red else Color.Green)
            .clickable {
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        when (sharingState) {
            LocationSharingState.IDLE -> {
                Text(
                    text = "Share Location",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            LocationSharingState.LOADING -> {
                CircularLoading()
            }

            LocationSharingState.SHARING -> {
                if (isLocationPointsEmpty) {
                    CircularLoading()
                } else {
                    Text(
                        text = "Stop Sharing Location",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

            }

        }

    }
}