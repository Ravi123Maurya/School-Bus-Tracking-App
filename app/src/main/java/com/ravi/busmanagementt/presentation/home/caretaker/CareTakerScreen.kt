package com.ravi.busmanagementt.presentation.home.caretaker


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

// Feature Data Model
data class CaretakerFeature(
    val icon: ImageVector,
    val label: String,
    val cardColor: Color,
    val route: String
)

// Caretaker Features Configuration
object CaretakerFeatures {
    fun getAllFeatures(): List<CaretakerFeature> = listOf(
        CaretakerFeature(
            icon = Icons.Default.CheckCircle,
            label = "Attendance",
            cardColor = Color(0xFFEC4899),
            route = NavRoutes.MARK_ATTENDANCE_SCREEN
        ),
        CaretakerFeature(
            icon = Icons.Default.People,
            label = "View Children",
            cardColor = Color(0xFF8B5CF6),
            route = NavRoutes.VIEW_CHILDREN_SCREEN
        ),
        CaretakerFeature(
            icon = Icons.Default.EventNote,
            label = "Daily Reports",
            cardColor = Color(0xFF3B82F6),
            route = ""
        ),
        CaretakerFeature(
            icon = Icons.Default.Schedule,
            label = "My Schedule",
            cardColor = Color(0xFF06B6D4),
            route = ""
        )
    )
}

// Main Caretaker Screen
@Composable
fun CaretakerScreen(
    modifier: Modifier = Modifier,
    navController: NavController
) {

    val context = LocalContext.current
    val features = CaretakerFeatures.getAllFeatures()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        CaretakerHeader()

        // Feature Grid
        features.chunked(2).forEach { rowFeatures ->
            FeatureRow(
                features = rowFeatures,
                onFeatureClick = { feature ->
                    if (feature.route == "") {
                        context.showToast("Coming Soon!")
                    } else {
                        navController.navigate(feature.route)
                    }

                }
            )
        }
    }
}

@Composable
private fun CaretakerHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Caretaker Dashboard",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Manage daily care activities",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF64748B)
        )
    }
}

@Composable
private fun FeatureRow(
    features: List<CaretakerFeature>,
    onFeatureClick: (CaretakerFeature) -> Unit
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
    icon: ImageVector = Icons.Default.Favorite,
    label: String = "Feature",
    cardColor: Color = Color(0xFFEC4899),
    modifier: Modifier = Modifier,
    onCardClick: () -> Unit = {}
) {
    Card(
        modifier = modifier.aspectRatio(1.2f),
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

// Alternative: Compact Version (like your original)
@Composable
fun CompactFeatureCard(
    icon: ImageVector = Icons.Default.Favorite,
    label: String = "Feature",
    cardColor: Color = Color(0xFFEC4899),
    modifier: Modifier = Modifier,
    onCardClick: () -> Unit = {}
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        ),
        onClick = onCardClick,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .width(120.dp)
                .height(100.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                modifier = Modifier.size(32.dp),
                imageVector = icon,
                contentDescription = "feature",
                tint = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

// Preview
@Preview(showBackground = true, backgroundColor = 0xFFF8FAFC)
@Composable
private fun CaretakerScreenPreview() {
    MaterialTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            CaretakerHeader()
            Spacer(modifier = Modifier.height(16.dp))

            // Sample Grid
            val sampleFeatures = CaretakerFeatures.getAllFeatures().take(4)
            sampleFeatures.chunked(2).forEach { rowFeatures ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    rowFeatures.forEach { feature ->
                        FeatureCard(
                            icon = feature.icon,
                            label = feature.label,
                            cardColor = feature.cardColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}