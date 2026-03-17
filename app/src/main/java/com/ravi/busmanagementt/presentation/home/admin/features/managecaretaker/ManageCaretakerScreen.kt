package com.ravi.busmanagementt.presentation.home.admin.features.managecaretaker

import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.google.firebase.firestore.GeoPoint
import com.ravi.busmanagementt.domain.model.Caretaker
import com.ravi.busmanagementt.presentation.components.CircularLoading
import com.ravi.busmanagementt.presentation.components.NavBackScaffold
import com.ravi.busmanagementt.ui.theme.AppColors
import com.ravi.busmanagementt.utils.showToast


// Form state for adding/editing Caretaker
data class CaretakerFormState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val assignedBusId: String = "",
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false
)

data class CaretakerFormErrors(
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val busIdError: String? = null,
)

// Edit form state (only editable fields)
data class EditCaretakerFormState(
    val name: String = "",
    val assignedBusId: String = "",
)

data class EditCaretakerFormErrors(
    val nameError: String? = null,
    val busIdError: String? = null
)

// Main Screen
@Composable
fun ManageCaretakerScreen(
    navController: NavController,
    manageCaretakerViewModel: ManageCaretakerViewModel = hiltViewModel(),
    busId: String? = null
) {

    val context = LocalContext.current
    var editingCaretaker by remember { mutableStateOf<Caretaker?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Caretaker?>(null) }


    val state by manageCaretakerViewModel.state.collectAsStateWithLifecycle()
    val availableBusIds by manageCaretakerViewModel.allBusIds.collectAsStateWithLifecycle()

    var showBusDropdown by remember { mutableStateOf(false) }


    LaunchedEffect(state) {
        state.errorMsg?.let { msg ->
            context.showToast(msg)
        }
        state.successMsg?.let { msg ->
            context.showToast(msg)
        }
    }



    ManageCaretakerContent(
        isFetchingCaretakers = state.isLoading,
        caretakers = state.caretakers,
        onAddCaretaker = {
            manageCaretakerViewModel.onEvent(CaretakerEvents.OnShowDialog(true))
        },
        onEditCaretaker = { caretakerId ->
            // Navigate to edit screen or show edit dialog
            editingCaretaker = state.caretakers.find { it.id == caretakerId }
            manageCaretakerViewModel.onEvent(CaretakerEvents.OnShowDialog(true))
        },
        onDeleteCaretaker = { caretaker ->
            showDeleteDialog = caretaker
        },
        onCaretakerClick = { caretakerId ->
            // Navigate to Caretaker details
        },
        onBackClick = { navController.popBackStack() }
    )



    if (state.showDialog) {
        AddCaretakerDialog(
            showBusDropdown = showBusDropdown,
            availableBusIds = availableBusIds,
            isLoading = state.isLoading,
            defaultBusId = busId,
            onDismiss = {
                manageCaretakerViewModel.onEvent(CaretakerEvents.OnShowDialog(false))
            },
            onSave = { caretaker ->
                // Create Caretaker
                manageCaretakerViewModel.onEvent(CaretakerEvents.AddNewCaretaker(caretaker))
            },
            onDropdownChange = { showBusDropdown = it }
        )
    }

    if (state.showDialog){
        editingCaretaker?.let { caretaker ->
            EditCaretakerDialog(
                isLoading = state.isLoading,
                showBusDropdown = showBusDropdown,
                caretaker = caretaker,
                onDismiss = { editingCaretaker = null },
                onSave = { updatedCaretaker ->
                    // Update Caretaker in firestore database
                    manageCaretakerViewModel.onEvent(CaretakerEvents.UpdateCaretaker(updatedCaretaker))
                },
                availableBusIds = availableBusIds,
                onDropdownChange = { showBusDropdown = it }
            )
        }
    }


    // Delete Confirmation Dialog
    showDeleteDialog?.let { caretaker ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Caretaker") },
            text = {
                Text("Are you sure you want to delete ${caretaker.name}? This action cannot be undone.")
            },
            confirmButton = {

                Button(
                    onClick = {
//                        manageCaretakerViewModel.deleteCaretaker(caretaker.id, caretaker.email)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53935)
                    )
                ) {
                    if (state.isLoading) {
                        CircularLoading()
                    } else {
                        Text("Delete")
                    }
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
private fun ManageCaretakerContent(
    isFetchingCaretakers: Boolean,
    caretakers: List<Caretaker>,
    onAddCaretaker: () -> Unit,
    onEditCaretaker: (String) -> Unit,
    onDeleteCaretaker: (Caretaker) -> Unit,
    onCaretakerClick: (String) -> Unit,
    onBackClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedBusFilter by remember { mutableStateOf("All") }

    val filteredCaretakers = remember(caretakers, searchQuery, selectedBusFilter) {
        caretakers.filter { Caretaker ->
            val matchesSearch = searchQuery.isEmpty() ||
                    Caretaker.name.contains(searchQuery, ignoreCase = true) ||
                    Caretaker.email.contains(searchQuery, ignoreCase = true)

            val matchesBus = selectedBusFilter == "All" ||
                    Caretaker.assignedBusId == selectedBusFilter

            matchesSearch && matchesBus
        }
    }

    val availableBuses = remember(caretakers) {
        listOf("All") + caretakers.map { it.assignedBusId }.distinct().sorted()
    }

    NavBackScaffold(
        barTitle = "Manage Caretakers",
        onBackClick = onBackClick,
        fabIcon = Icons.Default.Add,
        onFabClick = onAddCaretaker
    ) { paddingValues ->

        if (isFetchingCaretakers) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularLoading(color = AppColors.Primary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Stats Header
                CaretakersStatsHeader(
                    totalCaretakers = caretakers.size,
                    filteredCaretakers = filteredCaretakers.size
                )

                // Search and Filter
                CaretakerSearchAndFilter(
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    selectedBus = selectedBusFilter,
                    availableBuses = availableBuses,
                    onBusFilterChange = { selectedBusFilter = it }
                )

                // Caretakers List
                if (filteredCaretakers.isEmpty()) {
                    EmptyCaretakersState(
                        hasSearch = searchQuery.isNotEmpty(),
                        onAddClick = onAddCaretaker
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(
                            items = filteredCaretakers,
                            key = { it.id }
                        ) { filteredCaretaker ->
                            CaretakerItem(
                                caretaker = filteredCaretaker,
                                onClick = { onCaretakerClick(filteredCaretaker.id) },
                                onEdit = { onEditCaretaker(filteredCaretaker.id) },
                                onDelete = {
                                    onDeleteCaretaker(filteredCaretaker)
                                }
                            )

                            if (filteredCaretaker.id != filteredCaretakers.last().id) {
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

}

@Composable
private fun CaretakersStatsHeader(
    totalCaretakers: Int,
    filteredCaretakers: Int
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
                label = "Total Caretakers",
                value = totalCaretakers.toString(),
                icon = Icons.Default.People,
                modifier = Modifier.weight(1f)
            )

            Spacer(Modifier.width(16.dp))

            StatCard(
                label = "Showing",
                value = filteredCaretakers.toString(),
                icon = Icons.Default.FilterList,
                valueColor = AppColors.Primary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    icon: ImageVector,
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
private fun CaretakerSearchAndFilter(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedBus: String,
    availableBuses: List<String>,
    onBusFilterChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search by name or email...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search")
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        // Bus filter
        if (availableBuses.size > 1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsBus,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )

                Text(
                    text = "Filter:",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableBuses) { busId ->
                        FilterChip(
                            selected = selectedBus == busId,
                            onClick = { onBusFilterChange(busId) },
                            label = { Text(busId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyCaretakersState(
    hasSearch: Boolean,
    onAddClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (hasSearch) Icons.Default.SearchOff else Icons.Default.PersonOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color.Gray
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = if (hasSearch) "No Caretakers found" else "No Caretakers added yet",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Gray
        )

        if (!hasSearch) {
            Spacer(Modifier.height(8.dp))

            Text(
                text = "Add Caretakers to assign them to buses",
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

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
                Text("Add First Caretaker")
            }
        }
    }
}

@Composable
private fun CaretakerItem(
    caretaker: Caretaker,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    ListItem(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        headlineContent = {
            Text(
                text = caretaker.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        },
        supportingContent = {
            Column(modifier = Modifier.padding(top = 4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color.Gray
                    )
                    Text(
                        text = caretaker.email,
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }

                Spacer(Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsBus,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = AppColors.Primary
                    )
                    Text(
                        text = "Bus: ${caretaker.assignedBusId}",
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
                    .background(AppColors.Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = caretaker.name.take(2).uppercase(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Primary
                )
            }
        },
        trailingContent = {
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
                            onEdit()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null)
                        }
                    )

                    // Todo : Implement delete caretaker feature
//                    DropdownMenuItem(
//                        text = { Text("Delete", color = Color(0xFFE53935)) },
//                        onClick = {
//                            showMenu = false
//                            onDelete()
//                        },
//                        leadingIcon = {
//                            Icon(
//                                Icons.Default.Delete,
//                                contentDescription = null,
//                                tint = Color(0xFFE53935)
//                            )
//                        }
//                    )
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCaretakerDialog(
    showBusDropdown: Boolean = false,
    availableBusIds: List<String>,
    isLoading: Boolean = false,
    defaultBusId: String?,
    onDismiss: () -> Unit,
    onSave: (Caretaker) -> Unit,
    onDropdownChange: (Boolean) -> Unit,
) {
    var formState by remember {
        mutableStateOf(
            CaretakerFormState(assignedBusId = defaultBusId ?: "")
        )
    }
    var errors by remember { mutableStateOf(CaretakerFormErrors()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Caretaker") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.heightIn(max = 500.dp)
            ) {

                // Caretaker Name Field
                item {
                    OutlinedTextField(
                        value = formState.name,
                        onValueChange = {
                            formState = formState.copy(name = it)
                            errors = errors.copy(nameError = null)
                        },
                        label = { Text("Caretaker Name") },
                        placeholder = { Text("Ravi Maurya") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null)
                        },
                        isError = errors.nameError != null,
                        supportingText = errors.nameError?.let { { Text(it) } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Caretaker Email Field
                item {
                    OutlinedTextField(
                        value = formState.email,
                        onValueChange = {
                            formState = formState.copy(email = it)
                            errors = errors.copy(emailError = null)
                        },
                        label = { Text("Email") },
                        placeholder = { Text("Caretaker@example.com") },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null)
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        isError = errors.emailError != null,
                        supportingText = errors.emailError?.let { { Text(it) } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Caretaker Password Field
                item {
                    OutlinedTextField(
                        value = formState.password,
                        onValueChange = {
                            formState = formState.copy(password = it)
                            errors = errors.copy(passwordError = null)
                        },
                        label = { Text("Password") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null)
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    formState = formState.copy(
                                        isPasswordVisible = !formState.isPasswordVisible
                                    )
                                }
                            ) {
                                Icon(
                                    imageVector = if (formState.isPasswordVisible)
                                        Icons.Default.VisibilityOff
                                    else
                                        Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        visualTransformation = if (formState.isPasswordVisible)
                            VisualTransformation.None
                        else
                            PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        isError = errors.passwordError != null,
                        supportingText = errors.passwordError?.let { { Text(it) } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Caretaker Confirm Password Field
                item {
                    OutlinedTextField(
                        value = formState.confirmPassword,
                        onValueChange = {
                            formState = formState.copy(confirmPassword = it)
                            errors = errors.copy(confirmPasswordError = null)
                        },
                        label = { Text("Confirm Password") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null)
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    formState = formState.copy(
                                        isConfirmPasswordVisible = !formState.isConfirmPasswordVisible
                                    )
                                }
                            ) {
                                Icon(
                                    imageVector = if (formState.isConfirmPasswordVisible)
                                        Icons.Default.VisibilityOff
                                    else
                                        Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        visualTransformation = if (formState.isConfirmPasswordVisible)
                            VisualTransformation.None
                        else
                            PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        isError = errors.confirmPasswordError != null,
                        supportingText = errors.confirmPasswordError?.let { { Text(it) } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Caretaker Assign Bus ID Field with Dropdown
                item {
                    ExposedDropdownMenuBox(
                        expanded = showBusDropdown,
                        onExpandedChange = { onDropdownChange(it) }
                    ) {
                        OutlinedTextField(
                            readOnly = true,
                            value = formState.assignedBusId,
                            onValueChange = {
                                formState = formState.copy(assignedBusId = it)
                                errors = errors.copy(busIdError = null)
                            },
                            label = { Text("Assigned Bus ID") },
                            placeholder = { Text("Select Bus ID") },
                            leadingIcon = {
                                Icon(Icons.Default.DirectionsBus, contentDescription = null)
                            },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = showBusDropdown)
                            },
                            isError = errors.busIdError != null,
                            supportingText = errors.busIdError?.let { { Text(it) } },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = showBusDropdown,
                            onDismissRequest = { onDropdownChange(false) }
                        ) {

                            if (availableBusIds.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No bus found") },
                                    onClick = { onDropdownChange(false) })
                            }

                            availableBusIds.forEach { busId ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DirectionsBus,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp),
                                                tint = AppColors.Primary
                                            )
                                            Text(busId)
                                        }
                                    },
                                    onClick = {
                                        formState = formState.copy(assignedBusId = busId)
                                        errors = errors.copy(busIdError = null)
                                        onDropdownChange(false)
                                    }
                                )
                            }
                        }
                    }
                }


            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val validationErrors = validateCaretakerForm(formState)
                    if (validationErrors.hasErrors()) {
                        errors = validationErrors
                    } else {
                        val caretaker = Caretaker(
                            name = formState.name,
                            email = formState.email,
                            password = formState.password,
                            assignedBusId = formState.assignedBusId,
                            id = formState.name
                        )
                        onSave(caretaker)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Primary
                )
            ) {
                if (isLoading) {
                    CircularLoading()
                } else {
                    Text("Add Caretaker")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditCaretakerDialog(
    isLoading: Boolean,
    showBusDropdown: Boolean,
    caretaker: Caretaker,
    onDismiss: () -> Unit,
    onSave: (Caretaker) -> Unit,
    onDropdownChange: (Boolean) -> Unit,
    availableBusIds: List<String>
) {
    var formState by remember {
        mutableStateOf(
            EditCaretakerFormState(
                name = caretaker.name,
                assignedBusId = caretaker.assignedBusId,
            )
        )
    }
    var errors by remember { mutableStateOf(EditCaretakerFormErrors()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = AppColors.Primary
                )
                Text("Edit Caretaker")
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                // Email (read-only)
                item {
                    OutlinedTextField(
                        value = caretaker.email,
                        onValueChange = {},
                        label = { Text("Email") },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null)
                        },
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = Color.Gray,
                            disabledBorderColor = Color.LightGray,
                            disabledLeadingIconColor = Color.Gray,
                            disabledLabelColor = Color.Gray
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Text(
                        text = "Email cannot be changed",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                item {
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                }

                // Editable: Name
                item {
                    OutlinedTextField(
                        value = formState.name,
                        onValueChange = {
                            formState = formState.copy(name = it)
                            errors = errors.copy(nameError = null)
                        },
                        label = { Text("Caretaker Name") },
                        placeholder = { Text("Ravi Maurya") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null)
                        },
                        isError = errors.nameError != null,
                        supportingText = errors.nameError?.let { { Text(it) } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Caretaker Assign Bus ID Field with Dropdown
                item {
                    ExposedDropdownMenuBox(
                        expanded = showBusDropdown,
                        onExpandedChange = { onDropdownChange(it) }
                    ) {
                        OutlinedTextField(
                            readOnly = true,
                            value = formState.assignedBusId,
                            onValueChange = {
                                formState = formState.copy(assignedBusId = it)
                                errors = errors.copy(busIdError = null)
                            },
                            label = { Text("Assigned Bus ID") },
                            placeholder = { Text("Select Bus ID") },
                            leadingIcon = {
                                Icon(Icons.Default.DirectionsBus, contentDescription = null)
                            },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = showBusDropdown)
                            },
                            isError = errors.busIdError != null,
                            supportingText = errors.busIdError?.let { { Text(it) } },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = showBusDropdown,
                            onDismissRequest = { onDropdownChange(false) }
                        ) {

                            if (availableBusIds.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No bus found") },
                                    onClick = { onDropdownChange(false) })
                            }

                            availableBusIds.forEach { busId ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DirectionsBus,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp),
                                                tint = AppColors.Primary
                                            )
                                            Text(busId)
                                        }
                                    },
                                    onClick = {
                                        formState = formState.copy(assignedBusId = busId)
                                        errors = errors.copy(busIdError = null)
                                        onDropdownChange(false)
                                    }
                                )
                            }
                        }
                    }
                }

            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val validationErrors = validateEditCaretakerForm(formState)
                    if (validationErrors.hasErrors()) {
                        errors = validationErrors
                    } else {
                        val updatedCaretaker = caretaker.copy(
                            name = formState.name,
                            assignedBusId = formState.assignedBusId
                        )
                        onSave(updatedCaretaker)
                    }
                },
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
                    Text("Save Changes")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Validation for edit form
private fun validateEditCaretakerForm(state: EditCaretakerFormState): EditCaretakerFormErrors {
    var errors = EditCaretakerFormErrors()

    if (state.name.isBlank()) {
        errors = errors.copy(nameError = "Name is required")
    }

    if (state.assignedBusId.isBlank()) {
        errors = errors.copy(busIdError = "Bus ID is required")
    }


    return errors
}

private fun EditCaretakerFormErrors.hasErrors(): Boolean {
    return nameError != null || busIdError != null
}

// Validation
private fun validateCaretakerForm(state: CaretakerFormState): CaretakerFormErrors {
    var errors = CaretakerFormErrors()

    if (state.name.isBlank()) {
        errors = errors.copy(nameError = "Name is required")
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

    if (state.confirmPassword.isBlank()) {
        errors = errors.copy(confirmPasswordError = "Please confirm password")
    } else if (state.password != state.confirmPassword) {
        errors = errors.copy(confirmPasswordError = "Passwords do not match")
    }

    if (state.assignedBusId.isBlank()) {
        errors = errors.copy(busIdError = "Bus ID is required")
    }

    return errors
}

private fun CaretakerFormErrors.hasErrors(): Boolean {
    return nameError != null ||
            emailError != null ||
            passwordError != null ||
            confirmPasswordError != null ||
            busIdError != null
}
