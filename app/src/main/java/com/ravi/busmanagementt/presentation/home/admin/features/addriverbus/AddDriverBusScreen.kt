package com.ravi.busmanagementt.presentation.home.admin.features.addriverbus

import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
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
import com.ravi.busmanagementt.presentation.viewmodels.AddDriverBusState
import com.ravi.busmanagementt.presentation.viewmodels.AddDriverBusViewModel
import com.ravi.busmanagementt.ui.theme.AppColors
import com.ravi.busmanagementt.utils.showToast


// Main Screen
@Composable
fun AddDriverBusScreen(
    navController: NavController,
    addDriverBusViewModel: AddDriverBusViewModel = hiltViewModel()
) {

    val context = LocalContext.current
    val addDriverBusState by addDriverBusViewModel.addDriverBusState.collectAsStateWithLifecycle()

    LaunchedEffect(addDriverBusState) {
        when (val state = addDriverBusState) {
            is AddDriverBusState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
            }

            AddDriverBusState.Idle -> {

            }

            AddDriverBusState.Loading -> {

            }

            is AddDriverBusState.Success -> {
                Toast.makeText(context, "Driver & Bus added successfully", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    AddDriverBusContent(
        isLoading = addDriverBusState is AddDriverBusState.Loading,
        onSaveClick = { busAndDriver ->
            addDriverBusViewModel.addDriverBus(busAndDriver)
        },
        onCancelClick = { navController.popBackStack() }
    )
}

@Composable
private fun AddDriverBusContent(
    isLoading: Boolean,
    onSaveClick: (BusAndDriver) -> Unit,
    onCancelClick: () -> Unit
) {
    val context = LocalContext.current
    var formState by remember { mutableStateOf(DriverBusFormState()) }
    var errors by remember { mutableStateOf(FormErrors()) }
    var showRouteDialog by remember { mutableStateOf(false) }

    NavBackScaffold(
        barTitle = "Add Bus & Driver",
        onBackClick = onCancelClick
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Form content
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Driver Information Section
                item {
                    SectionHeader(
                        title = "Driver Information",
                        icon = Icons.Default.Person
                    )
                }

                item {
                    DriverNameField(
                        value = formState.driverName,
                        onValueChange = {
                            formState = formState.copy(driverName = it)
                            errors = errors.copy(driverNameError = null)
                        },
                        error = errors.driverNameError
                    )
                }

                item {
                    EmailField(
                        value = formState.email,
                        onValueChange = {
                            formState = formState.copy(email = it)
                            errors = errors.copy(emailError = null)
                        },
                        error = errors.emailError
                    )
                }

                item {
                    PasswordField(
                        value = formState.password,
                        onValueChange = {
                            formState = formState.copy(password = it)
                            errors = errors.copy(passwordError = null)
                        },
                        isVisible = formState.isPasswordVisible,
                        onVisibilityToggle = {
                            formState = formState.copy(
                                isPasswordVisible = !formState.isPasswordVisible
                            )
                        },
                        error = errors.passwordError,
                        label = "Password"
                    )
                }


                // Bus Information Section
                item {
                    Spacer(Modifier.height(8.dp))
                    SectionHeader(
                        title = "Bus Information",
                        icon = Icons.Default.DirectionsBus
                    )
                }

                item {
                    BusIdField(
                        value = formState.busId,
                        onValueChange = {
                            formState = formState.copy(busId = it)
                            errors = errors.copy(busIdError = null)
                        },
                        error = errors.busIdError
                    )
                }

                // Routes Section
                item {
                    Spacer(Modifier.height(8.dp))
                    SectionHeader(
                        title = "Bus Routes",
                        icon = Icons.Default.Route,
                        action = {
                            TextButton(onClick = { showRouteDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Add Route")
                            }
                        }
                    )
                }

                if (formState.routes.isEmpty()) {
                    item {
                        EmptyRoutesCard(
                            onAddClick = { showRouteDialog = true }
                        )
                    }
                } else {
                    itemsIndexed(
                        items = formState.routes,
                        key = {i, route -> "$i-${route.geoPoint}" }
                    ) { i, route ->
                        RouteCard(
                            route = route,
                            onDelete = {
                                formState = formState.copy(
                                    routes = formState.routes - route
                                )
                            }
                        )
                    }
                }

                if (errors.routesError != null) {
                    item {
                        Text(
                            text = errors.routesError!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }

            // Bottom action buttons
            ActionButtons(
                isLoading = isLoading,
                onSaveClick = {
                    val validationErrors = validateForm(formState)
                    if (validationErrors.hasErrors()) {
                        errors = validationErrors
                    } else {
                        val busAndDriver = BusAndDriver(
                            id = 0, // Will be generated by backend
                            driverName = formState.driverName,
                            email = formState.email,
                            password = formState.password,
                            busId = formState.busId,
                            routes = formState.routes
                        )
                        onSaveClick(busAndDriver)
                    }
                },
                onCancelClick = onCancelClick
            )
        }
    }

    // Route Dialog
    if (showRouteDialog) {
        AddRouteDialog(
            onDismiss = { showRouteDialog = false },
            onAdd = { route ->
                if(formState.routes.contains(route)){
                   context.showToast("Route already added")
                    return@AddRouteDialog
                }
                formState = formState.copy(
                    routes = formState.routes + route
                )
                errors = errors.copy(routesError = null)
                showRouteDialog = false
            }
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: ImageVector,
    action: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
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

        action?.invoke()
    }
}

@Composable
private fun DriverNameField(
    value: String,
    onValueChange: (String) -> Unit,
    error: String?
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Driver Name") },
        placeholder = { Text("Enter driver's full name") },
        leadingIcon = {
            Icon(Icons.Default.Person, contentDescription = null)
        },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun EmailField(
    value: String,
    onValueChange: (String) -> Unit,
    error: String?
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Email") },
        placeholder = { Text("driver@example.com") },
        leadingIcon = {
            Icon(Icons.Default.Email, contentDescription = null)
        },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next
        ),
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    isVisible: Boolean,
    onVisibilityToggle: () -> Unit,
    error: String?,
    label: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = { Text("Enter password") },
        leadingIcon = {
            Icon(Icons.Default.Lock, contentDescription = null)
        },
        trailingIcon = {
            IconButton(onClick = onVisibilityToggle) {
                Icon(
                    imageVector = if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (isVisible) "Hide password" else "Show password"
                )
            }
        },
        visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Next
        ),
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun BusIdField(
    value: String,
    onValueChange: (String) -> Unit,
    error: String?
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Bus ID / Number") },
        placeholder = { Text("BUS-001") },
        leadingIcon = {
            Icon(Icons.Default.DirectionsBus, contentDescription = null)
        },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun EmptyRoutesCard(onAddClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.LightGray.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
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
                text = "No routes added yet",
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
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
                Text("Add First Route")
            }
        }
    }
}

@Composable
private fun RouteCard(
    route: BusStop,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(AppColors.Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = AppColors.Primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = route.stopName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )

                Text(
                    text = route.location,
                    fontSize = 13.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "Lat: ${route.geoPoint.latitude}, Lng: ${route.geoPoint.longitude}",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete route",
                    tint = Color(0xFFE53935)
                )
            }
        }
    }
}

@Composable
private fun AddRouteDialog(
    onDismiss: () -> Unit,
    onAdd: (BusStop) -> Unit
) {

    var routeStopForm by remember { mutableStateOf(RouteStopForm()) }
    var routeStopErrors by remember { mutableStateOf(RouteStopForErrors()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Route Stop") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = routeStopForm.stopName,
                    onValueChange = { routeStopForm = routeStopForm.copy(stopName = it) },
                    label = { Text("Stop Name") },
                    placeholder = { Text("Main Street Stop") },
                    isError = routeStopErrors.stopNameError != null,
                    supportingText = routeStopErrors.stopNameError?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = routeStopForm.location,
                    onValueChange = { routeStopForm = routeStopForm.copy(location = it) },
                    label = { Text("Location Address") },
                    placeholder = { Text("123 Main St, City") },
                    isError = routeStopErrors.locationError != null,
                    supportingText = routeStopErrors.locationError?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = routeStopForm.latitude,
                        onValueChange = { routeStopForm = routeStopForm.copy(latitude = it) },
                        label = { Text("Latitude") },
                        placeholder = { Text("00.0000") },
                        isError = routeStopErrors.latitudeError != null,
                        supportingText = routeStopErrors.latitudeError?.let { { Text(it) } },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        ),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = routeStopForm.longitude,
                        onValueChange = { routeStopForm = routeStopForm.copy(longitude = it) },
                        label = { Text("Longitude") },
                        placeholder = { Text("00.0000") },
                        isError = routeStopErrors.longitudeError != null,
                        supportingText = routeStopErrors.longitudeError?.let { { Text(it) } },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        ),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
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
                        onAdd(
                            BusStop(
                                stopName = routeStopForm.stopName,
                                location = routeStopForm.location,
                                GeoPoint(
                                    routeStopForm.latitude.toDouble(),
                                    routeStopForm.longitude.toDouble()
                                )
                            )
                        )
                    }

                },
                enabled = routeStopForm.stopName.isNotEmpty() &&
                        routeStopForm.location.isNotEmpty() &&
                        routeStopForm.latitude.isNotEmpty() &&
                        routeStopForm.longitude.isNotEmpty()
            ) {
                Text("Add")
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
    isLoading: Boolean,
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
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Primary
                )
            ) {
                if (isLoading) {
                    CircularLoading()
                } else {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Save Driver & Bus")
                }

            }
        }
    }
}

// Validation
private fun validateForm(state: DriverBusFormState): FormErrors {
    var errors = FormErrors()

    if (state.driverName.isBlank()) {
        errors = errors.copy(driverNameError = "Driver name is required")
    }

    if (state.email.isBlank()) {
        errors = errors.copy(emailError = "Email is required")
    } else if (!Patterns.EMAIL_ADDRESS.matcher(state.email).matches()) {
        errors = errors.copy(emailError = "Invalid email format")
    }

    if (state.password.isBlank()) {
        errors = errors.copy(passwordError = "Password is required")
    } else if (state.password.length < 6) {
        errors = errors.copy(passwordError = "Password must be at least 6 characters")
    }


    if (state.busId.isBlank()) {
        errors = errors.copy(busIdError = "Bus ID is required")
    }

    if (state.routes.isEmpty()) {
        errors = errors.copy(routesError = "At least one route is required")
    }

    return errors
}

private fun validateRouteStop(state: RouteStopForm): RouteStopForErrors {
    var errors = RouteStopForErrors()
    if (state.stopName.isBlank()) {
        errors = errors.copy(stopNameError = "Stop name is required")
    }
    if (state.location.isBlank()) {
        errors = errors.copy(locationError = "Location is required")
    }

    // validate latitude
    val latitude = state.latitude.toDoubleOrNull() // Safely convert to Double or null
    when {
        state.latitude.isBlank() -> {
            errors = errors.copy(latitudeError = "Latitude is required")
        }

        latitude == null -> {
            // This catches invalid formats like ".", "-", or "abc"
            errors = errors.copy(latitudeError = "Invalid number format")
        }

        latitude !in -90.0..90.0 -> {
            // Check if the number is within the valid geographical range
            errors = errors.copy(latitudeError = "Must be between -90 and 90")
        }
    }

    // validate longitude
    val longitude = state.longitude.toDoubleOrNull() // Safely convert to Double or null
    when {
        state.longitude.isBlank() -> {
            errors = errors.copy(longitudeError = "Longitude is required")
        }

        longitude == null -> {
            errors = errors.copy(longitudeError = "Invalid number format")
        }

        longitude !in -180.0..180.0 -> {
            errors = errors.copy(longitudeError = "Must be between -180 and 180")
        }
    }
    return errors
}


// UI State
data class DriverBusFormState(
    val driverName: String = "",
    val email: String = "",
    val password: String = "",
    val busId: String = "BUS_4001",
    val routes: List<BusStop> = emptyList(),
    val isPasswordVisible: Boolean = true,
    val isConfirmPasswordVisible: Boolean = true
)

data class FormErrors(
    val driverNameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val busIdError: String? = null,
    val routesError: String? = null
)

data class RouteStopForm(
    val stopName: String = "",
    val location: String = "",
    val latitude: String = "19.1",
    val longitude: String = "73.0"
)

data class RouteStopForErrors(
    val stopNameError: String? = null,
    val locationError: String? = null,
    val latitudeError: String? = null,
    val longitudeError: String? = null
)


private fun RouteStopForErrors.hasErrors(): Boolean {
    return stopNameError != null ||
            locationError != null ||
            latitudeError != null ||
            longitudeError != null
}

private fun FormErrors.hasErrors(): Boolean {
    return driverNameError != null ||
            emailError != null ||
            passwordError != null ||
            busIdError != null ||
            routesError != null
}