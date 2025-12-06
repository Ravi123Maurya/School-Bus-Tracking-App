package com.ravi.busmanagementt.presentation.components

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ravi.busmanagementt.ui.theme.AppColors
import com.ravi.busmanagementt.utils.showToast


@Preview
@Composable
fun CameraAnimateFaB(
    modifier: Modifier = Modifier,
    onBusLocationClick: () -> Unit = {},
    onMyLocationClick: () -> Unit = {}
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .wrapContentSize()
            .padding(16.dp)
    ) {


        FloatingActionButton(
            onClick = {
                onBusLocationClick()

            },
            shape = CircleShape,
            containerColor = Color.Blue
        ) {
            Icon(Icons.Default.DirectionsBus, "Bus Location", tint = Color.White)
        }

        Spacer(Modifier.height(12.dp))


        FloatingActionButton(
            onClick = onMyLocationClick,
            shape = CircleShape,
            containerColor = AppColors.Primary
        ) {
            Icon(Icons.Default.MyLocation, "My Location", tint = Color.White)
        }
    }

}
