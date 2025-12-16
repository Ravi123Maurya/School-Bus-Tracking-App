package com.ravi.busmanagementt.presentation.home.admin

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ravi.busmanagementt.presentation.navigation.NavRoutes
import com.ravi.busmanagementt.utils.showToast


// Main Admin Portal
@Composable
fun AdminPortal(
    modifier: Modifier = Modifier,
    navController: NavController
) {
    val features = AdminFeatures.getAllFeatures()
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Portal Header
        AdminPortalHeader()

        // Feature Grid
        features.chunked(2).forEach { rowFeatures ->
            FeatureRow(
                features = rowFeatures,
                onFeatureClick = { feature ->
                    if (feature.route.isNotEmpty())
                    navController.navigate(feature.route)
                    else{
                        context.showToast("Coming soon...")
                    }
                }
            )
        }
    }
}

@Composable
private fun AdminPortalHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Admin Dashboard",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Manage your system",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF64748B)
        )
    }
}

@Composable
private fun FeatureRow(
    features: List<AdminFeature>,
    onFeatureClick: (AdminFeature) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        features.forEach { feature ->
            FeatureCard(
                icon = feature.icon,
                label = feature.label,
                cardColor = feature.cardColor,
                modifier = Modifier.weight(1f),
                onCardClick = { onFeatureClick(feature) }
            )
        }

        // Fill empty space if odd number
        if (features.size == 1) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun FeatureCard(
    icon: ImageVector = Icons.Default.DirectionsBus,
    label: String = "Feature",
    cardColor: Color = Color(0xFF3B82F6),
    modifier: Modifier = Modifier,
    onCardClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .aspectRatio(1.2f),
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        onClick = onCardClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                modifier = Modifier.size(40.dp),
                imageVector = icon,
                contentDescription = label,
                tint = Color.White
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = label,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

// Alternative: Compact Card Version
@Composable
fun CompactFeatureCard(
    icon: ImageVector = Icons.Default.DirectionsBus,
    label: String = "Feature",
    cardColor: Color = Color(0xFF3B82F6),
    modifier: Modifier = Modifier,
    onCardClick: () -> Unit = {}
) {
    Card(
        modifier = modifier.height(100.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        onClick = onCardClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                modifier = Modifier.size(32.dp),
                imageVector = icon,
                contentDescription = label,
                tint = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}


// Feature Data Model
data class AdminFeature(
    val icon: ImageVector,
    val label: String,
    val cardColor: Color,
    val route: String
)

// Admin Features Configuration
object AdminFeatures {
    fun getAllFeatures(): List<AdminFeature> = listOf(
        AdminFeature(
            icon = Icons.Default.DirectionsBus,
            label = "Track Buses",
            cardColor = Color(0xFF3B82F6),
            route = NavRoutes.ALL_BUSES_SCREEN
        ),
        AdminFeature(
            icon = Icons.Default.People,
            label = "Parents",
            cardColor = Color(0xFF8B5CF6),
            route = NavRoutes.MANAGE_PARENT_SCREEN
        ),
        AdminFeature(
            icon = Icons.Default.PersonAdd,
            label = "Add Bus",
            cardColor = Color(0xFF10B981),
            route = NavRoutes.ADD_DRIVER_BUS_SCREEN
        ),
        AdminFeature(
            icon = Icons.Default.Route,
            label = "Buses/Routes",
            cardColor = Color(0xFFF59E0B),
            route = NavRoutes.BUSES_AND_ROUTES_SCREEN
        ),
        AdminFeature(
            icon = Icons.Default.Assessment,
            label = "Reports",
            cardColor = Color(0xFF06B6D4),
            route = NavRoutes.REPORTS_SCREEN
        ),
        AdminFeature(
            icon = Icons.Default.ChatBubble,
            label = "Announcement",
            cardColor = Color(0xFFEC4899),
            route = ""
        )
    )
}
