package com.ravi.busmanagementt.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.DensityMedium
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ravi.busmanagementt.ui.theme.AppColors
import com.ravi.busmanagementt.utils.showToast


@Preview
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusTopAppBar(
    isMapExpanded: Boolean = true,
    isParentPortal: Boolean = true,
    title: String = "BUS TRACK",
    onSettingClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onBackClick: () -> Unit = {}
) {

    val context = LocalContext.current

    TopAppBar(
        navigationIcon = {
            if (isMapExpanded) {
                IconButton(
                    onClick = onBackClick
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBackIosNew,
                        contentDescription = "go back",
                        tint = Color.White
                    )
                }
            }
        },
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                letterSpacing = 8.sp,
                color = Color.White
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = AppColors.Primary,
        ),
        actions = {

//            TopBarAction(icon = Icons.Default.NotificationsNone, true, onClick = {
//                context.showToast("Coming soon...")
//                onNotificationClick()
//            })
//            Spacer(Modifier.width(12.dp))

            if (isParentPortal)
            TopBarAction(icon = Icons.Default.DensityMedium, onClick = onSettingClick)

        }
    )
}

@Composable
fun TopBarAction(
    icon: ImageVector,
    hasNotification: Boolean = false,
    onClick: () -> Unit = {}
) {

    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        BadgedBox(
            badge = {
                if (hasNotification)
                    Badge(
                        containerColor = Color.Green,
                        contentColor = Color.White
                    ) {
                        Text("4")
                    }
            }
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "notification",
                tint = Color.White
            )
        }


    }
}