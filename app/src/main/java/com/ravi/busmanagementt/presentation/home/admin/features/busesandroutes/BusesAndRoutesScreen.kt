package com.ravi.busmanagementt.presentation.home.admin.features.busesandroutes

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.google.firebase.firestore.GeoPoint
import com.ravi.busmanagementt.domain.model.BusAndDriver
import com.ravi.busmanagementt.domain.model.BusStop
import com.ravi.busmanagementt.presentation.components.CircularLoading
import com.ravi.busmanagementt.presentation.components.NavBackScaffold
import com.ravi.busmanagementt.presentation.home.admin.features.allbuses.AllBusesViewModel
import com.ravi.busmanagementt.presentation.home.admin.features.allbuses.Bus
import com.ravi.busmanagementt.presentation.home.admin.features.allbuses.GetAllBusesState
import com.ravi.busmanagementt.presentation.navigation.EditBusAndStopsScreen
import com.ravi.busmanagementt.presentation.navigation.NavRoutes
import com.ravi.busmanagementt.ui.theme.AppColors
import com.ravi.busmanagementt.utils.showToast

// Main Screen
@Composable
fun BusesAndRoutesScreen(
    navController: NavController,
    allBusesViewModel: AllBusesViewModel = hiltViewModel()
) {

    val context = LocalContext.current
    val allBusesWithLiveStatus by allBusesViewModel.busesWithStatus.collectAsStateWithLifecycle()
    var allBuses by remember { mutableStateOf(emptyList<BusAndDriver>()) }
    var isFetching by remember { mutableStateOf(false) }

    LaunchedEffect(allBusesWithLiveStatus) {
        when (val state = allBusesWithLiveStatus) {
            is GetAllBusesState.Error -> {
                context.showToast(state.message)
                isFetching = false
            }

            GetAllBusesState.Loading -> {
                isFetching = true
            }

            is GetAllBusesState.Success -> {
                allBuses = state.buses.map { busDetailWithStatus ->
                    busDetailWithStatus.busDetail
                }
                isFetching = false
            }
        }
    }

    NavBackScaffold(
        barTitle = "Buses and Routes",
        onBackClick = { navController.popBackStack() },
        fabIcon = Icons.Default.Add,
        onFabClick = { navController.navigate(NavRoutes.ADD_DRIVER_BUS_SCREEN) }
    ) { paddingValues ->

        if (isFetching) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularLoading(color = AppColors.Primary)
            }
        } else {
            if (allBuses.isNotEmpty()) {
                BusesAndRoutesContent(
                    modifier = Modifier.padding(paddingValues),
                    busesWithDrivers = allBuses,
                    onEditClick = { busId ->
                        navController.navigate(
                            EditBusAndStopsScreen(
                                busId = busId
                            )
                        )
                    },
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No Data Found",
                        color = Color.Gray,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

    }


}

@Composable
private fun BusesAndRoutesContent(
    modifier: Modifier = Modifier,
    busesWithDrivers: List<BusAndDriver>,
    onEditClick: (String) -> Unit,
) {

    var searchQuery by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf<BusAndDriver?>(null) }
    var expandedBusId by remember { mutableStateOf<Int?>(null) }

    val filteredBuses = remember(busesWithDrivers, searchQuery) {
        busesWithDrivers.filter { bus ->
            searchQuery.isEmpty() ||
                    bus.busId.contains(searchQuery, ignoreCase = true) ||
                    bus.driverName.contains(searchQuery, ignoreCase = true)
        }
    }


    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        // Header Stats
        BusesStatsHeader(
            totalBuses = busesWithDrivers.size,
            totalRoutes = busesWithDrivers.sumOf { it.routes.size }
        )

        // Search Bar
        SearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it }
        )

        // Buses List
        if (filteredBuses.isEmpty()) {
            EmptyBusesState(hasSearch = searchQuery.isNotEmpty())
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                itemsIndexed(
                    items = filteredBuses,
                    key = { i, data -> "$i-${data.busId}" }
                ) { i, bus ->
                    BusWithRoutesCard(
                        bus = bus,
                        isExpanded = expandedBusId == i,
                        onExpandClick = {
                            expandedBusId = if (expandedBusId == i) null else i
                        },
                        onEditClick = { onEditClick(bus.busId) },
                        onDeleteClick = {
                            showDeleteDialog = bus
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }


    // Delete Confirmation Dialog
    showDeleteDialog?.let { bus ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFE53935),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("Delete Bus & Driver?") },
            text = {
                Text(
                    "Are you sure you want to delete ${bus.busId} (${bus.driverName})? " +
                            "This will also remove all ${bus.routes.size} route stops. This action cannot be undone."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        // TODO: Delete bus
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53935)
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun BusesStatsHeader(
    totalBuses: Int,
    totalRoutes: Int
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
                label = "Total Buses",
                value = totalBuses.toString(),
                icon = Icons.Default.DirectionsBus,
                modifier = Modifier.weight(1f)
            )

            Spacer(Modifier.width(16.dp))

            StatCard(
                label = "Route Stops",
                value = totalRoutes.toString(),
                icon = Icons.Default.LocationOn,
                valueColor = Color(0xFF4CAF50),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    valueColor: Color = AppColors.Primary,
    modifier: Modifier = Modifier
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
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        placeholder = { Text("Search by bus ID or driver name...") },
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
}

@Composable
private fun EmptyBusesState(hasSearch: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (hasSearch) Icons.Default.SearchOff else Icons.Default.DirectionsBus,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color.Gray
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = if (hasSearch) "No buses found" else "No buses added yet",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Gray
        )
    }
}

@Composable
private fun BusWithRoutesCard(
    bus: BusAndDriver,
    isExpanded: Boolean,
    onExpandClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .animateContentSize()
        ) {

            // Bus Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onExpandClick)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bus Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(AppColors.Primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsBus,
                        contentDescription = null,
                        tint = AppColors.Primary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(Modifier.width(16.dp))

                // Bus Info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = bus.busId,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color.Gray
                        )
                        Text(
                            text = bus.driverName,
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Route,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = AppColors.Primary
                        )
                        Text(
                            text = "${bus.routes.size} stops",
                            fontSize = 13.sp,
                            color = AppColors.Primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Menu Button
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options"
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = {
                                showMenu = false
                                onEditClick()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = null)
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Delete", color = Color(0xFFE53935)) },
                            onClick = {
                                showMenu = false
                                onDeleteClick()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = Color(0xFFE53935)
                                )
                            }
                        )
                    }
                }

                // Expand icon
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = Color.Gray
                )
            }

            // Expanded Routes Section
            if (isExpanded) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = Color.LightGray.copy(alpha = 0.3f)
                )

                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = AppColors.Primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Route Stops",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    if (bus.routes.isEmpty()) {
                        Text(
                            text = "No route stops added",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        bus.routes.forEachIndexed { index, stop ->
                            RouteStopItem(
                                stop = stop,
                                index = index + 1,
                                isLast = index == bus.routes.lastIndex
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RouteStopItem(
    stop: BusStop,
    index: Int,
    isLast: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // Timeline indicator
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Number badge
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(AppColors.Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = index.toString(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Primary
                )
            }

            // Connecting line
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(40.dp)
                        .background(AppColors.Primary.copy(alpha = 0.3f))
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        // Stop details
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (!isLast) 8.dp else 0.dp)
        ) {
            Text(
                text = stop.stopName,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )

            if (stop.location.isNotEmpty()) {
                Text(
                    text = stop.location,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = null,
                    modifier = Modifier.size(10.dp),
                    tint = Color.Gray
                )
                Text(
                    text = "${stop.geoPoint.latitude}, ${stop.geoPoint.longitude}",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

// Sample Data
fun getSampleBusesWithDrivers() = listOf(
    BusAndDriver(
        id = 1,
        driverName = "Rajesh Kumar",
        email = "rajesh@school.com",
        password = "password123",
        busId = "BUS_4001",
        routes = listOf(
            BusStop("Main Gate", "School Main Entrance", GeoPoint(19.0760, 72.8777)),
            BusStop("Park Avenue", "Near City Park", GeoPoint(19.0820, 72.8750)),
            BusStop("Mall Road", "Shopping Complex", GeoPoint(19.0700, 72.8800)),
            BusStop("Railway Station", "Central Station", GeoPoint(19.0850, 72.8690))
        )
    ),
    BusAndDriver(
        id = 2,
        driverName = "Priya Sharma",
        email = "priya@school.com",
        password = "password123",
        busId = "BUS-002",
        routes = listOf(
            BusStop("Highway Stop", "NH 48 Junction", GeoPoint(19.0900, 72.8600)),
            BusStop("Temple Road", "Old Temple Area", GeoPoint(19.0750, 72.8820)),
            BusStop("Market Square", "Central Market", GeoPoint(19.0680, 72.8750))
        )
    ),
    BusAndDriver(
        id = 3,
        driverName = "Amit Patel",
        email = "amit@school.com",
        password = "password123",
        busId = "BUS-003",
        routes = listOf(
            BusStop("Airport Road", "Near Terminal 2", GeoPoint(19.0950, 72.8550)),
            BusStop("Business Park", "IT Hub", GeoPoint(19.0800, 72.8780)),
            BusStop("Residential Complex", "Green Valley", GeoPoint(19.0720, 72.8810)),
            BusStop("School Gate 2", "Back Entrance", GeoPoint(19.0760, 72.8770)),
            BusStop("Sports Complex", "City Stadium", GeoPoint(19.0840, 72.8720))
        )
    ),
    BusAndDriver(
        id = 4,
        driverName = "Sneha Desai",
        email = "sneha@school.com",
        password = "password123",
        busId = "BUS-004",
        routes = emptyList()
    )
)