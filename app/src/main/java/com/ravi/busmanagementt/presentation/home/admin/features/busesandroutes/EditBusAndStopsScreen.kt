package com.ravi.busmanagementt.presentation.home.admin.features.busesandroutes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.firebase.firestore.GeoPoint
import com.ravi.busmanagementt.domain.model.BusAndDriver
import com.ravi.busmanagementt.domain.model.BusStop
import com.ravi.busmanagementt.presentation.components.CircularLoading
import com.ravi.busmanagementt.presentation.components.NavBackScaffold
import com.ravi.busmanagementt.presentation.home.admin.features.addriverbus.RouteStopForErrors
import com.ravi.busmanagementt.presentation.home.admin.features.addriverbus.RouteStopForm
import com.ravi.busmanagementt.presentation.home.admin.features.addriverbus.hasErrors
import com.ravi.busmanagementt.presentation.home.admin.features.addriverbus.validateRouteStop
import com.ravi.busmanagementt.ui.theme.AppColors
import com.ravi.busmanagementt.utils.showToast

// Edit State
data class EditBusState(
    val busId: String = "", // Todo: BusId and Email cannot be changed (TextField is read-only, disabled, editable in future if needed)
    val driverName: String = "",
    val email: String = "",
    val routes: List<BusStop> = emptyList()
)

data class EditBusErrors(
    val busIdError: String? = null,
    val driverNameError: String? = null,
    val emailError: String? = null
)

// Main Screen
@Composable
fun EditBusAndStopsScreen(
    navController: NavController,
    busId: String? = null,
    editViewModel: EditBusAndStopsViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        if (busId != null) {
            editViewModel.getBusData(busId)
        }
    }

    val context = LocalContext.current
    val busDataState by editViewModel.busDataState.collectAsState()
    val updateBusDataState by editViewModel.updateBusDataState.collectAsState()
    val deleteBusStopState by editViewModel.deleteBusStopState.collectAsState()
    var isFetchingData by remember { mutableStateOf(busDataState is GetBusDataState.Loading) }
    var busData by remember { mutableStateOf<BusAndDriver?>(null) }
    var updatedBusData by remember { mutableStateOf<BusAndDriver?>(null) }


    // Get Bus Data State
    LaunchedEffect(busDataState) {
        when (val state = busDataState) {
            is GetBusDataState.Success -> {
                busData = state.busData
                isFetchingData = false
                editViewModel.resetAllStates()
            }

            is GetBusDataState.Error -> {
                context.showToast(state.message)
                isFetchingData = false
                editViewModel.resetAllStates()
            }

            GetBusDataState.Loading -> {}
            GetBusDataState.Idle -> {}
        }
    }

    LaunchedEffect(updateBusDataState) {
        when (val state = updateBusDataState) {
            is UpdateBusDataState.Error -> {
                context.showToast(state.message)
                editViewModel.resetAllStates()
            }

            UpdateBusDataState.Idle -> {}
            UpdateBusDataState.Loading -> {}
            is UpdateBusDataState.Success -> {
                context.showToast(state.message)
                editViewModel.resetAllStates()
                updatedBusData = null
            }
        }
    }

    LaunchedEffect(deleteBusStopState) {
        when (val state = deleteBusStopState) {
            is DeleteBusStopState.Error -> {
                context.showToast(state.message)
                editViewModel.resetAllStates()
            }

            DeleteBusStopState.Idle -> {}
            DeleteBusStopState.Loading -> {}
            is DeleteBusStopState.Success -> {
                context.showToast(state.message)
                editViewModel.resetAllStates()
            }
        }
    }

    NavBackScaffold(
        barTitle = "Edit Bus & Routes",
        onBackClick = { navController.popBackStack() }
    ) { paddingValues ->

        if (isFetchingData) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularLoading(color = AppColors.Primary)
            }
        } else {
            if (busData != null) {
                EditBusAndStopsContent(
                    modifier = Modifier.padding(paddingValues),
                    initialBus = busData!!,
                    onSaveClick = { updatedBus ->
                        updatedBusData = updatedBus
                    },
                    onCancelClick = { navController.popBackStack() }
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

    if (updatedBusData != null) {
        FinalUpdateAlertDialog(
            hasConfirmClicked = updateBusDataState is UpdateBusDataState.Loading,
            onConfirm = {
                editViewModel.updateBusData(updatedBusData!!)
            },
            onDismiss = {
                updatedBusData = null
            }
        )
    }

}

@Composable
private fun EditBusAndStopsContent(
    modifier: Modifier = Modifier,
    initialBus: BusAndDriver,
    onSaveClick: (BusAndDriver) -> Unit,
    onCancelClick: () -> Unit
) {

    val context = LocalContext.current
    var editState by remember {
        mutableStateOf(
            EditBusState(
                busId = initialBus.busId,
                driverName = initialBus.driverName,
                email = initialBus.email,
                routes = initialBus.routes.toMutableList()
            )
        )
    }
    var errors by remember { mutableStateOf(EditBusErrors()) }
    var showAddStopDialog by remember { mutableStateOf(false) }
    var editingStopIndex by remember { mutableStateOf<Int?>(null) }
    var deletingStopIndex by remember { mutableStateOf<Int?>(null) }
    var hasChanges by remember { mutableStateOf(false) }

    // Track changes
    LaunchedEffect(editState) {
        hasChanges = editState.busId != initialBus.busId ||
                editState.driverName != initialBus.driverName ||
                editState.email != initialBus.email ||
                editState.routes != initialBus.routes
    }

    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        // Content
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Bus Information Section
            item {
                SectionHeader(
                    title = "Bus Information",
                    icon = Icons.Default.DirectionsBus
                )
            }

            item {
                BusInfoCard(
                    busId = editState.busId,
                    driverName = editState.driverName,
                    email = editState.email,
                    errors = errors,
                    onBusIdChange = {
                        editState = editState.copy(busId = it)
                        errors = errors.copy(busIdError = null)
                    },
                    onDriverNameChange = {
                        editState = editState.copy(driverName = it)
                        errors = errors.copy(driverNameError = null)
                    },
                    onEmailChange = {
                        editState = editState.copy(email = it)
                        errors = errors.copy(emailError = null)
                    }
                )
            }

            // Routes Section
            item {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader(
                        title = "Route Stops (${editState.routes.size})",
                        icon = Icons.Default.Route
                    )

                    Button(
                        onClick = { showAddStopDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Primary
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Add Stop", fontSize = 14.sp)
                    }
                }
            }

            if (editState.routes.isEmpty()) {
                item {
                    EmptyRoutesCard(
                        onAddClick = { showAddStopDialog = true }
                    )
                }
            } else {
                itemsIndexed(
                    items = editState.routes,
                    key = { index, _ -> index }
                ) { index, stop ->
                    EditableRouteStopCard(
                        stop = stop,
                        index = index + 1,
                        isLast = index == editState.routes.lastIndex,
                        onEditClick = { editingStopIndex = index },
                        onDeleteClick = { deletingStopIndex = index }
                    )
                }
            }
        }

        // Action Buttons
        ActionButtons(
            hasChanges = hasChanges,
            onSaveClick = {
                val validationErrors = validateBusInfo(editState)
                if (validationErrors.hasErrors()) {
                    errors = validationErrors
                } else {
                    val updatedBus = initialBus!!.copy(
                        busId = editState.busId,
                        driverName = editState.driverName,
                        email = editState.email,
                        routes = editState.routes
                    )
                    onSaveClick(updatedBus)
                }
            },
            onCancelClick = onCancelClick
        )
    }


    // Todo : Save Button is not enabling after filling form
    // Add Stop Dialog
    if (showAddStopDialog) {
        AddEditStopDialog(
            stop = null,
            onDismiss = { showAddStopDialog = false },
            onSave = { newStop ->
                if (editState.routes.contains(newStop)) {
                    context.showToast("Stop already exists")
                    return@AddEditStopDialog
                } else {
                    editState = editState.copy(
                        routes = editState.routes + newStop
                    )
                    showAddStopDialog = false
                }

            }
        )
    }

    // Todo: Geopoints aren't showing changed in edit screen
    // Edit Stop Dialog
    editingStopIndex?.let { index ->
        AddEditStopDialog(
            stop = editState.routes[index],
            onDismiss = { editingStopIndex = null },
            onSave = { updatedStop ->
                val updatedRoutes = editState.routes.toMutableList()
                updatedRoutes[index] = updatedStop
                editState = editState.copy(routes = updatedRoutes)
                editingStopIndex = null
            }
        )
    }

    // Delete Confirmation Dialog
    deletingStopIndex?.let { index ->
        AlertDialog(
            onDismissRequest = { deletingStopIndex = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFE53935),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("Delete Stop?") },
            text = {
                Text(
                    "Are you sure you want to delete \"${editState.routes[index].stopName}\" bus stop?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updatedRoutes = editState.routes.toMutableList()
                        updatedRoutes.removeAt(index)
                        editState = editState.copy(routes = updatedRoutes)
                        deletingStopIndex = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53935)
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingStopIndex = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AppColors.Primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black
        )
    }
}

@Composable
private fun BusInfoCard(
    busId: String,
    driverName: String,
    email: String,
    errors: EditBusErrors,
    onBusIdChange: (String) -> Unit,
    onDriverNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            /*** Bus Information ***/
            // Bus Id
            OutlinedTextField(
                value = busId,
                onValueChange = onBusIdChange,
                label = { Text("Bus ID") },
                placeholder = { Text("BUS-001") },
                leadingIcon = {
                    Icon(Icons.Default.DirectionsBus, contentDescription = null)
                },
                isError = errors.busIdError != null,
                supportingText = errors.busIdError?.let { { Text(it) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                readOnly = true,
                enabled = false
            )
            Text(
                text = "Bus ID cannot be changed",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(start = 4.dp)
            )
            // Driver Name
            OutlinedTextField(
                value = driverName,
                onValueChange = onDriverNameChange,
                label = { Text("Driver Name") },
                placeholder = { Text("Ravi Maurya") },
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = null)
                },
                isError = errors.driverNameError != null,
                supportingText = errors.driverNameError?.let { { Text(it) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            // Email
            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                label = { Text("Email") },
                placeholder = { Text("driver@example.com") },
                leadingIcon = {
                    Icon(Icons.Default.Email, contentDescription = null)
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done
                ),
                isError = errors.emailError != null,
                supportingText = errors.emailError?.let { { Text(it) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                readOnly = true,
                enabled = false
            )
            Text(
                text = "Email cannot be changed",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

@Composable
private fun EmptyRoutesCard(onAddClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.LightGray.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Route,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color.Gray
            )

            Text(
                text = "No route stops added",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Add First Stop")
            }
        }
    }
}

@Composable
private fun EditableRouteStopCard(
    stop: BusStop,
    index: Int,
    isLast: Boolean,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Timeline indicator
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(AppColors.Primary.copy(alpha = 0.1f))
                        .border(2.dp, AppColors.Primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = index.toString(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Primary
                    )
                }

                if (!isLast) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(20.dp)
                            .background(AppColors.Primary.copy(alpha = 0.3f))
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            // Stop details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stop.stopName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )

                if (stop.location.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = Color.Gray
                        )
                        Text(
                            text = stop.location,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = Color.Gray
                    )
                    Text(
                        text = "${stop.geoPoint.latitude}, ${stop.geoPoint.longitude}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }

            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit stop",
                        tint = AppColors.Primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete stop",
                        tint = Color(0xFFE53935),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AddEditStopDialog(
    stop: BusStop?,
    onDismiss: () -> Unit,
    onSave: (BusStop) -> Unit
) {

    var routeStopForm by remember { mutableStateOf(RouteStopForm()) }
    var routeStopErrors by remember { mutableStateOf(RouteStopForErrors()) }


    val isEdit = stop != null
    LaunchedEffect(stop) {
        if (stop != null) {
            routeStopForm = routeStopForm.copy(
                stopName = stop.stopName,
                location = stop.location,
                latitude = stop.geoPoint.latitude.toString(),
                longitude = stop.geoPoint.longitude.toString()
            )
        }
    }
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isEdit) Icons.Default.Edit else Icons.Default.Add,
                        contentDescription = null,
                        tint = AppColors.Primary
                    )
                    Text(if (isEdit) "Edit Stop" else "Add Stop")
                }
            },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.heightIn(max = 450.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = routeStopForm.stopName,
                            onValueChange = { routeStopForm = routeStopForm.copy(stopName = it) },
                            label = { Text("Stop Name") },
                            placeholder = { Text("Main Street Stop") },
                            leadingIcon = {
                                Icon(Icons.Default.LocationOn, contentDescription = null)
                            },
                            isError = routeStopErrors.stopNameError != null,
                            supportingText = routeStopErrors.stopNameError?.let { { Text(it) } },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = routeStopForm.location,
                            onValueChange = { routeStopForm = routeStopForm.copy(location = it) },
                            label = { Text("Location Address") },
                            placeholder = { Text("123 Main St, City") },
                            leadingIcon = {
                                Icon(Icons.Default.Place, contentDescription = null)
                            },
                            isError = routeStopErrors.locationError != null,
                            supportingText = routeStopErrors.locationError?.let { { Text(it) } },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Text(
                            text = "GPS Coordinates",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = routeStopForm.latitude,
                                onValueChange = {
                                    routeStopForm = routeStopForm.copy(latitude = it)
                                },
                                label = { Text("Latitude") },
                                placeholder = { Text("19.0760") },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal
                                ),
                                isError = routeStopErrors.latitudeError != null,
                                supportingText = routeStopErrors.latitudeError?.let { { Text(it) } },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = routeStopForm.longitude,
                                onValueChange = {
                                    routeStopForm = routeStopForm.copy(longitude = it)
                                },
                                label = { Text("Longitude") },
                                placeholder = { Text("72.8777") },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal
                                ),
                                isError = routeStopErrors.longitudeError != null,
                                supportingText = routeStopErrors.longitudeError?.let { { Text(it) } },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val validationErrors = validateRouteStop(routeStopForm)
                        if (validationErrors.hasErrors()) {
                            routeStopErrors = validationErrors
                        } else {
                            val lat = routeStopForm.latitude.toDouble()
                            val lng = routeStopForm.longitude.toDouble()
                            onSave(
                                BusStop(
                                    stopName = routeStopForm.stopName,
                                    location = routeStopForm.location,
                                    geoPoint = GeoPoint(lat, lng)
                                )
                            )
                        }

                    },
                    enabled = true,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Primary
                    )
                ) {
                    Text(if (isEdit) "Save" else "Add")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }

    @Composable
    private fun ActionButtons(
        hasChanges: Boolean,
        onSaveClick: () -> Unit,
        onCancelClick: () -> Unit
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 8.dp,
            color = Color.White
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancelClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = onSaveClick,
                    modifier = Modifier.weight(1f),
                    enabled = hasChanges,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Save Changes")
                }
            }
        }
    }


    @Preview(showBackground = true)
    @Composable
    private fun FinalUpdateAlertDialog(
        hasConfirmClicked: Boolean = false,
        onConfirm: () -> Unit = {},
        onDismiss: () -> Unit = {}
    ) {

        var verifyPassword by remember { mutableStateOf("") }
        var hasPasswordVerified by remember(verifyPassword) { mutableStateOf(verifyPassword == "123456") }

        AlertDialog(
            title = {
                Text("Final Update")
            },
            text = {
                Column {
                    Text("Are you sure you want to update the changes? This action cannot be undone.")
                    Spacer(Modifier.height(16.dp))
                    TextField(
                        value = verifyPassword,
                        onValueChange = { verifyPassword = it },
                        placeholder = { Text("Enter password to confirm") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Primary
                    ),
                    enabled = hasPasswordVerified,
                ) {
                    if (hasConfirmClicked) {
                        CircularLoading()
                    } else {
                        Text("Confirm Changes")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            },
            onDismissRequest = onDismiss,
        )
    }

    // Validation
    private fun validateBusInfo(state: EditBusState): EditBusErrors {
        var errors = EditBusErrors()

        if (state.busId.isBlank()) {
            errors = errors.copy(busIdError = "Bus ID is required")
        }

        if (state.driverName.isBlank()) {
            errors = errors.copy(driverNameError = "Driver name is required")
        }

        if (state.email.isBlank()) {
            errors = errors.copy(emailError = "Email is required")
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(state.email).matches()) {
            errors = errors.copy(emailError = "Invalid email format")
        }

        return errors
    }

    private fun EditBusErrors.hasErrors(): Boolean {
        return busIdError != null || driverNameError != null || emailError != null
    }