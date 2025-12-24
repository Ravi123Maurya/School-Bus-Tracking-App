package com.ravi.busmanagementt.presentation.home.admin.features.allbuses

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.ravi.busmanagementt.presentation.components.CircularLoading
import com.ravi.busmanagementt.presentation.components.NavBackScaffold
import com.ravi.busmanagementt.presentation.navigation.BusStopsScreen
import com.ravi.busmanagementt.presentation.navigation.HomeScreen
import com.ravi.busmanagementt.presentation.navigation.NavRoutes
import com.ravi.busmanagementt.ui.theme.AppColors
import kotlin.collections.emptyList

// Data models
data class Bus(
    val id: Int,
    val name: String,
    val number: String,
    val driverName: String = "",
    val status: BusStatus = BusStatus.ACTIVE,
    val currentLocation: String = "",
    val totalStops: Int = 0,
    val completedStops: Int = 0,
    val eta: String? = null
)

enum class BusStatus {
    ALL,
    ACTIVE,
    IDLE,
}

// Main Screen
@Composable
fun AllBusesScreen(
    navController: NavController,
    allBusesViewModel: AllBusesViewModel = hiltViewModel(),
) {

    val context = LocalContext.current
    var buses by remember { mutableStateOf(emptyList<Bus>()) }
    val getAllBusesWithRTLocationStatus by allBusesViewModel.busesWithStatus.collectAsStateWithLifecycle()
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(getAllBusesWithRTLocationStatus) {
        when (val state = getAllBusesWithRTLocationStatus) {
            is GetAllBusesState.Error -> {
                Toast.makeText(context, "Error: ${state.message}", Toast.LENGTH_LONG).show()
                isLoading = false
            }

            GetAllBusesState.Loading -> {
                isLoading = true
            }

            is GetAllBusesState.Success -> {

                buses = state.buses.mapIndexed { i, busDetailWithStatus ->

                    val status =
                        if (busDetailWithStatus.isLive) BusStatus.ACTIVE else BusStatus.IDLE
                    val busDetail = busDetailWithStatus.busDetail
                    val realtimeLocations = busDetailWithStatus.realtimeLocations
                    var completedStops = 0
                    if (realtimeLocations.isNotEmpty()) {
                        completedStops = realtimeLocations.last().numberOfStopsReached
                    }

                    Bus(
                        id = i,
                        name = busDetail.driverName,
                        number = busDetail.busId,
                        driverName = busDetail.driverName,
                        status = status,
                        currentLocation = "",
                        totalStops = busDetail.routes.size,
                        completedStops = completedStops,
                        eta = null
                    )
                }
                isLoading = false
            }
        }
    }

    AllBusesContent(
        isLoading = isLoading,
        buses = buses,
        onSeeLocationClick = { busId ->
            navController.navigate(HomeScreen(busId = busId))
        },
        onSeeStopsClick = { busId ->
            navController.navigate(BusStopsScreen(busId = busId))
        },
        onBusClick = { busId ->
            // Navigate to bus details screen // todo
        },
        onNavBackClick = { navController.popBackStack() }
    )
}

@Composable
private fun AllBusesContent(
    isLoading: Boolean = false,
    buses: List<Bus>,
    onSeeLocationClick: (String) -> Unit,
    onSeeStopsClick: (String) -> Unit,
    onBusClick: (Int) -> Unit,
    onNavBackClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(BusStatus.ALL) }

    val filteredBuses = remember(buses, searchQuery, selectedFilter) {
        buses.filter { bus ->
            val matchesSearch = searchQuery.isEmpty() ||
                    bus.name.contains(searchQuery, ignoreCase = true) ||
                    bus.number.contains(searchQuery, ignoreCase = true)
            val matchesFilter = selectedFilter == BusStatus.ALL || bus.status == selectedFilter
            matchesSearch && matchesFilter
        }
    }

    NavBackScaffold(
        barTitle = "All Buses",
        onBackClick = onNavBackClick
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header with stats
            BusesHeader(
                totalBuses = buses.size,
                activeBuses = buses.count { it.status == BusStatus.ACTIVE }
            )

            // Search and filters
            BusSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                selectedFilter = selectedFilter,
                onFilterChange = { selectedFilter = it }
            )

            // Bus list
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularLoading(color = AppColors.Primary)
                }
            } else if (filteredBuses.isEmpty()) {
                EmptyBusesState(searchQuery = searchQuery)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(
                        items = filteredBuses,
                        key = { it.id },
                         contentType = { "bus_item" }
                    ) { bus ->
                        BusItem(
                            bus = bus,
                            onSeeLocationClick = { onSeeLocationClick(bus.number) },
                            onSeeStopsClick = { onSeeStopsClick(bus.number) },
                            onClick = { onBusClick(bus.id) }
                        )

                        if (bus.id != filteredBuses.last().id) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = Color.LightGray.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BusesHeader(
    totalBuses: Int,
    activeBuses: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AppColors.Primary.copy(alpha = 0.05f)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                label = "Total Buses",
                value = totalBuses.toString(),
                icon = Icons.Default.DirectionsBus
            )

            Spacer(Modifier.width(16.dp))

            StatCard(
                modifier = Modifier.weight(1f),
                label = "Active Now",
                value = activeBuses.toString(),
                icon = Icons.Default.CheckCircle,
                valueColor = Color(0xFF4CAF50)
            )
        }
    }
}


@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: ImageVector,
    valueColor: Color = AppColors.Primary
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = valueColor,
                modifier = Modifier.size(32.dp)
            )

            Spacer(Modifier.width(12.dp))

            Column {
                Text(
                    text = value,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = valueColor
                )
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun BusSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    selectedFilter: BusStatus,
    onFilterChange: (BusStatus) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Search field
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search buses...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search")
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        // Filter chips
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BusStatus.values().forEach { status ->
                FilterChip(
                    selected = selectedFilter == status,
                    onClick = { onFilterChange(status) },
                    label = { Text(status.name.lowercase().capitalize()) }
                )
            }
        }
    }
}

@Composable
private fun EmptyBusesState(searchQuery: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color.Gray
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = if (searchQuery.isEmpty()) {
                "No buses available"
            } else {
                "No buses found for \"$searchQuery\""
            },
            fontSize = 16.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun BusItem(
    bus: Bus,
    onSeeLocationClick: () -> Unit,
    onSeeStopsClick: () -> Unit,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        headlineContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = bus.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f, fill = false)
                )

                BusStatusBadge(status = bus.status)
            }
        },
        supportingContent = {
            Column(modifier = Modifier.padding(top = 4.dp)) {
                Text(
                    text = "Bus Number: ${bus.number}",
                    fontSize = 13.sp,
                    color = Color.Gray
                )

                if (bus.status == BusStatus.ACTIVE && bus.totalStops > 0) {
                    Spacer(Modifier.height(4.dp))
                    BusProgressIndicator(
                        completed = bus.completedStops,
                        total = bus.totalStops
                    )
                }

                if (bus.eta != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "ETA: ${bus.eta}",
                        fontSize = 12.sp,
                        color = AppColors.Primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        when (bus.status) {
                            BusStatus.ALL -> AppColors.Primary.copy(alpha = 0.1f)
                            BusStatus.ACTIVE -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                            BusStatus.IDLE -> Color.Gray.copy(alpha = 0.1f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsBus,
                    contentDescription = "bus icon",
                    modifier = Modifier.size(28.dp),
                    tint = when (bus.status) {
                        BusStatus.ALL -> AppColors.Primary
                        BusStatus.ACTIVE -> Color(0xFF4CAF50)
                        BusStatus.IDLE -> Color.Gray
                    }
                )
            }
        },
        trailingContent = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onSeeLocationClick,
                    enabled = bus.status == BusStatus.ACTIVE,
                    modifier = Modifier.height(36.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Location",
                        fontSize = 12.sp
                    )
                }

                OutlinedButton(
                    onClick = onSeeStopsClick,
                    modifier = Modifier.height(36.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Route,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Stops",
                        fontSize = 12.sp
                    )
                }
            }
        }
    )
}

@Composable
private fun BusStatusBadge(status: BusStatus) {
    val (backgroundColor, label) = when (status) {
        BusStatus.ALL -> Color.Transparent to "" // if Selected to ALL: don't show Status Badge
        BusStatus.ACTIVE -> Color(0xFF4CAF50) to "Active"
        BusStatus.IDLE -> Color.Gray to "Idle"
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = backgroundColor.copy(alpha = 0.15f)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = backgroundColor
        )
    }
}

@Composable
private fun BusProgressIndicator(completed: Int, total: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LinearProgressIndicator(
            progress = completed.toFloat() / total.toFloat(),
            modifier = Modifier
                .weight(1f)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = AppColors.Primary,
            trackColor = Color.LightGray.copy(alpha = 0.3f)
        )

        Text(
            text = "$completed/$total stops",
            fontSize = 11.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Medium
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun BusItemPreview(){
    val bus = Bus(
        id = 1,
        name = "Dipendra Vishwakarma",
        number = "12345",
        driverName = "driver name",
        status = BusStatus.ACTIVE,
        currentLocation = "123 Main St",
        totalStops = 10,
        completedStops = 5,
        eta = "5 mins"
    )

    BusItem(bus = bus, {}, {}) { }
}
