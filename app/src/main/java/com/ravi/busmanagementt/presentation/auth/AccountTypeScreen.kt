package com.ravi.busmanagementt.presentation.auth

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Person2
import androidx.compose.material.icons.filled.Person4
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ravi.busmanagementt.presentation.navigation.NavRoutes
import com.ravi.busmanagementt.presentation.viewmodels.PortalViewModel
import com.ravi.busmanagementt.ui.theme.AppColors
import com.ravi.busmanagementt.data.datastore.Portals
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState

import androidx.compose.foundation.verticalScroll

import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*

import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.ravi.busmanagementt.presentation.navigation.LoginScreen
import com.ravi.busmanagementt.utils.showToast


// Data Models
data class PortalInfo(
    val portal: Portals,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val gradient: List<Color>
)

// Portal Configuration
object PortalConfig {
    fun getPortalInfo(portal: Portals): PortalInfo {
        return when (portal) {
            Portals.PARENT -> PortalInfo(
                portal = Portals.PARENT,
                title = "Parent",
                description = "Monitor your child's activities",
                icon = Icons.Default.FamilyRestroom,
                gradient = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
            )

            Portals.DRIVER -> PortalInfo(
                portal = Portals.DRIVER,
                title = "Driver",
                description = "Manage routes and schedules",
                icon = Icons.Default.DirectionsCar,
                gradient = listOf(Color(0xFF3B82F6), Color(0xFF06B6D4))
            )

            Portals.CARETAKER -> PortalInfo(
                portal = Portals.CARETAKER,
                title = "Caretaker",
                description = "Track attendance and care",
                icon = Icons.Default.Favorite,
                gradient = listOf(Color(0xFFEC4899), Color(0xFFF59E0B))
            )

            Portals.ADMIN -> PortalInfo(
                portal = Portals.ADMIN,
                title = "Admin",
                description = "Full system management",
                icon = Icons.Default.AdminPanelSettings,
                gradient = listOf(Color(0xFF10B981), Color(0xFF059669))
            )
        }
    }
}

// Main Screen
@Composable
fun AccountTypeScreen(
    navController: NavController,
    portalViewModel: PortalViewModel = hiltViewModel()
) {

    val context = LocalContext.current

    AccountTypeContent(
        onParentAccountClick = {
            portalViewModel.setPortal(Portals.PARENT)
            navController.navigate(LoginScreen(portal = Portals.PARENT.name))
        },
        onDriverAccountClick = {
            portalViewModel.setPortal(Portals.DRIVER)
            navController.navigate(LoginScreen(portal = Portals.DRIVER.name))
        },
        onCaretakerAccountClick = {
            portalViewModel.setPortal(Portals.CARETAKER)
            navController.navigate(LoginScreen(portal = Portals.CARETAKER.name))
        },
        onAdminAccountClick = {
            portalViewModel.setPortal(Portals.ADMIN)
            navController.navigate(LoginScreen(portal = Portals.ADMIN.name))
        }
    )
}

@Composable
private fun AccountTypeContent(
    onParentAccountClick: () -> Unit = {},
    onDriverAccountClick: () -> Unit = {},
    onCaretakerAccountClick: () -> Unit = {},
    onAdminAccountClick: () -> Unit = {}
) {
    Scaffold(
        topBar = { PortalTopBar() },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            WelcomeHeader()

            Spacer(modifier = Modifier.height(24.dp))

            PortalCardsSection(
                onParentClick = onParentAccountClick,
                onDriverClick = onDriverAccountClick,
                onCaretakerClick = onCaretakerAccountClick,
                onAdminClick = onAdminAccountClick
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// Components
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PortalTopBar() {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = "Select Account Type",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.White,
            titleContentColor = Color(0xFF1E293B)
        )
    )
}

@Composable
private fun WelcomeHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 24.dp, vertical = 28.dp)
    ) {
        Text(
            text = "Welcome!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Choose your account type to get started",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF64748B),
            lineHeight = 24.sp
        )
    }
}

@Composable
private fun PortalCardsSection(
    onParentClick: () -> Unit,
    onDriverClick: () -> Unit,
    onCaretakerClick: () -> Unit,
    onAdminClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PortalCard(
            portalInfo = PortalConfig.getPortalInfo(Portals.PARENT),
            onClick = onParentClick
        )

        PortalCard(
            portalInfo = PortalConfig.getPortalInfo(Portals.DRIVER),
            onClick = onDriverClick
        )

        PortalCard(
            portalInfo = PortalConfig.getPortalInfo(Portals.CARETAKER),
            onClick = onCaretakerClick
        )

        PortalCard(
            portalInfo = PortalConfig.getPortalInfo(Portals.ADMIN),
            onClick = onAdminClick
        )
    }
}

@Composable
private fun PortalCard(
    portalInfo: PortalInfo,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Box with Gradient
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        brush = Brush.linearGradient(portalInfo.gradient)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = portalInfo.icon,
                    contentDescription = portalInfo.title,
                    modifier = Modifier.size(32.dp),
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            // Text Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = portalInfo.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = portalInfo.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF64748B),
                    lineHeight = 20.sp
                )
            }

            // Arrow Icon
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                modifier = Modifier.size(28.dp),
                tint = Color(0xFFCBD5E1)
            )
        }
    }
}


