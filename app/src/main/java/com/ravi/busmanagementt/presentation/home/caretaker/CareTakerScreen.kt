package com.ravi.busmanagementt.presentation.home.caretaker


import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ravi.busmanagementt.utils.showToast


// Main Caretaker Screen
@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true)
@Composable
fun CaretakerScreen(
    modifier: Modifier = Modifier,
    caretakerViewModel: CaretakerViewModel = hiltViewModel()
) {

    val context = LocalContext.current
    val state by caretakerViewModel.uiState.collectAsState()
    val isPickup by caretakerViewModel.isPickupRide.collectAsState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        CaretakerHeader()

        MarkAttendanceContent(
            isPickup = isPickup,
            children = state.students,
            onRideTypeClick = {
                caretakerViewModel.onEvent(UiEvent.OnRideTypeClick(it))
            },
            onStatusChange = { id, status ->
                caretakerViewModel.onEvent(UiEvent.MarkAttendance(id, status))
                caretakerViewModel.onEvent(UiEvent.GetStudents)
            }
        )
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

