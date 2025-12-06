package com.ravi.busmanagementt.presentation.home.admin.features.manageparents

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
import com.ravi.busmanagementt.domain.model.Parent
import com.ravi.busmanagementt.presentation.components.CircularLoading
import com.ravi.busmanagementt.presentation.components.NavBackScaffold
import com.ravi.busmanagementt.ui.theme.AppColors
import com.ravi.busmanagementt.utils.showToast


// Form state for adding/editing parent
data class ParentFormState(
    val name: String = "",
    val email: String = "@gmail.com",
    val password: String = "123456",
    val confirmPassword: String = "123456",
    val assignedBusId: String = "",
    val latitude: String = "12.12",
    val longitude: String = "12.21",
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false
)

data class ParentFormErrors(
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val busIdError: String? = null,
    val locationError: String? = null
)

// Edit form state (only editable fields)
data class EditParentFormState(
    val name: String = "",
    val assignedBusId: String = "",
    val latitude: String = "",
    val longitude: String = ""
)

data class EditParentFormErrors(
    val nameError: String? = null,
    val busIdError: String? = null,
    val locationError: String? = null
)

// Main Screen
@Composable
fun ManageParentsScreen(
    navController: NavController,
    manageParentViewModel: ManageParentViewModel = hiltViewModel(),
    busId: String? = null
) {

    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var editingParent by remember { mutableStateOf<Parent?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Parent?>(null) }

    val addParentState by manageParentViewModel.addParentState.collectAsStateWithLifecycle()
    val getParentState by manageParentViewModel.getParentState.collectAsStateWithLifecycle()
    val updateParentState by manageParentViewModel.updateParentState.collectAsStateWithLifecycle()
    val deleteParentState by manageParentViewModel.deleteParentState.collectAsStateWithLifecycle()

    var parents by remember { mutableStateOf(emptyList<Parent>()) }
    val availableBusIds by manageParentViewModel.allBusIds.collectAsStateWithLifecycle()

    var showBusDropdown by remember { mutableStateOf(false) }

    var isFetchingParents by remember { mutableStateOf(false) }
    // Listen for GET parents state changes
    LaunchedEffect(getParentState) {
        when (val state = getParentState) {
            is GetParentState.Error -> {
                isFetchingParents = false
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
            }

            GetParentState.Loading -> {
                isFetchingParents = true
            }

            is GetParentState.Success -> {
                isFetchingParents = false

                parents = state.parents
            }
        }
    }

    // Listen for ADD parent state changes
    LaunchedEffect(addParentState) {
        when (val state = addParentState) {
            is AddParentState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
            }

            AddParentState.Idle -> {}
            AddParentState.Loading -> {}
            is AddParentState.Success -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                showAddDialog = false
                manageParentViewModel.getAllParents()
                manageParentViewModel.resetAddParentState()
            }
        }
    }

    // Listen for UPDATE parent state changes
    LaunchedEffect(updateParentState) {
        when (val state = updateParentState) {
            UpdateParentState.Idle -> {}
            UpdateParentState.Loading -> {}

            is UpdateParentState.Error -> {
                context.showToast(state.message)
                editingParent = null
            }

            is UpdateParentState.Success -> {
                context.showToast(state.message)
                editingParent = null
                manageParentViewModel.getAllParents()
            }
        }
    }

    // Listen Delete Parent state changes
    LaunchedEffect(deleteParentState) {
        when (val state = deleteParentState) {
            DeleteParentState.Idle -> {}
            DeleteParentState.Loading -> {}
            is DeleteParentState.Error -> {
                context.showToast(state.message)
                showDeleteDialog = null
            }

            is DeleteParentState.Success -> {
                context.showToast(state.message)
                showDeleteDialog = null
                manageParentViewModel.getAllParents()
            }
        }
    }


    ManageParentsContent(
        isFetchingParents = isFetchingParents,
        parents = parents,
        onAddParent = { showAddDialog = true },
        onEditParent = { parentId ->
            // Navigate to edit screen or show edit dialog
            editingParent = parents.find { it.parentId == parentId }
        },
        onDeleteParent = { parent ->
            showDeleteDialog = parent
        },
        onParentClick = { parentId ->
            // Navigate to parent details
        },
        onBackClick = { navController.popBackStack() }
    )



    if (showAddDialog) {
        AddParentDialog(
            showBusDropdown = showBusDropdown,
            availableBusIds = availableBusIds,
            isLoading = addParentState is AddParentState.Loading,
            defaultBusId = busId,
            onDismiss = { showAddDialog = false },
            onSave = { parent ->
                // Create parent
                manageParentViewModel.addNewParent(parent)
            },
            onDropdownChange = { showBusDropdown = it }
        )
    }

    editingParent?.let { parent ->
        EditParentDialog(
            isLoading = updateParentState is UpdateParentState.Loading,
            showBusDropdown = showBusDropdown,
            parent = parent,
            onDismiss = { editingParent = null },
            onSave = { updatedParent ->
                // Update parent in firestore database
                manageParentViewModel.updateParent(updatedParent)
            },
            availableBusIds = availableBusIds,
            onDropdownChange = { showBusDropdown = it }
        )
    }

    // Delete Confirmation Dialog
    showDeleteDialog?.let { parent ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Parent") },
            text = {
                Text("Are you sure you want to delete ${parent.name}? This action cannot be undone.")
            },
            confirmButton = {

                Button(
                    onClick = {
                        manageParentViewModel.deleteParent(parent.parentId, parent.email)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53935)
                    )
                ) {
                    if (deleteParentState is DeleteParentState.Loading) {
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
private fun ManageParentsContent(
    isFetchingParents: Boolean,
    parents: List<Parent>,
    onAddParent: () -> Unit,
    onEditParent: (String) -> Unit,
    onDeleteParent: (Parent) -> Unit,
    onParentClick: (String) -> Unit,
    onBackClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedBusFilter by remember { mutableStateOf("All") }

    val filteredParents = remember(parents, searchQuery, selectedBusFilter) {
        parents.filter { parent ->
            val matchesSearch = searchQuery.isEmpty() ||
                    parent.name.contains(searchQuery, ignoreCase = true) ||
                    parent.email.contains(searchQuery, ignoreCase = true)

            val matchesBus = selectedBusFilter == "All" ||
                    parent.assignedBusId == selectedBusFilter

            matchesSearch && matchesBus
        }
    }

    val availableBuses = remember(parents) {
        listOf("All") + parents.map { it.assignedBusId }.distinct().sorted()
    }

    NavBackScaffold(
        barTitle = "Manage Parents",
        onBackClick = onBackClick,
        fabIcon = Icons.Default.Add,
        onFabClick = onAddParent
    ) { paddingValues ->

        if (isFetchingParents) {
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
                ParentsStatsHeader(
                    totalParents = parents.size,
                    filteredParents = filteredParents.size
                )

                // Search and Filter
                ParentSearchAndFilter(
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    selectedBus = selectedBusFilter,
                    availableBuses = availableBuses,
                    onBusFilterChange = { selectedBusFilter = it }
                )

                // Parents List
                if (filteredParents.isEmpty()) {
                    EmptyParentsState(
                        hasSearch = searchQuery.isNotEmpty(),
                        onAddClick = onAddParent
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(
                            items = filteredParents,
                            key = { it.parentId }
                        ) { filteredParent ->
                            ParentItem(
                                parent = filteredParent,
                                onClick = { onParentClick(filteredParent.parentId) },
                                onEdit = { onEditParent(filteredParent.parentId) },
                                onDelete = {
                                    onDeleteParent(filteredParent)
                                }
                            )

                            if (filteredParent.parentId != filteredParents.last().parentId) {
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
private fun ParentsStatsHeader(
    totalParents: Int,
    filteredParents: Int
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
                label = "Total Parents",
                value = totalParents.toString(),
                icon = Icons.Default.People,
                modifier = Modifier.weight(1f)
            )

            Spacer(Modifier.width(16.dp))

            StatCard(
                label = "Showing",
                value = filteredParents.toString(),
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
private fun ParentSearchAndFilter(
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
private fun EmptyParentsState(
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
            text = if (hasSearch) "No parents found" else "No parents added yet",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Gray
        )

        if (!hasSearch) {
            Spacer(Modifier.height(8.dp))

            Text(
                text = "Add parents to assign them to buses",
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
                Text("Add First Parent")
            }
        }
    }
}

@Composable
private fun ParentItem(
    parent: Parent,
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
                text = parent.name,
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
                        text = parent.email,
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
                        text = "Bus: ${parent.assignedBusId}",
                        fontSize = 12.sp,
                        color = AppColors.Primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color.Gray
                    )
                    Text(
                        text = "Stop: " + parent.stopName.ifEmpty {
                            "Not assigned"
                        },
                        fontSize = 11.sp,
                        color = Color.Gray
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
                    text = parent.name.take(2).uppercase(),
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

                    DropdownMenuItem(
                        text = { Text("Delete", color = Color(0xFFE53935)) },
                        onClick = {
                            showMenu = false
                            onDelete()
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
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddParentDialog(
    showBusDropdown: Boolean = false,
    availableBusIds: List<String>,
    isLoading: Boolean = false,
    defaultBusId: String?,
    onDismiss: () -> Unit,
    onSave: (Parent) -> Unit,
    onDropdownChange: (Boolean) -> Unit,
) {
    var formState by remember {
        mutableStateOf(
            ParentFormState(assignedBusId = defaultBusId ?: "")
        )
    }
    var errors by remember { mutableStateOf(ParentFormErrors()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Parent") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.heightIn(max = 500.dp)
            ) {

                // Parent Name Field
                item {
                    OutlinedTextField(
                        value = formState.name,
                        onValueChange = {
                            formState = formState.copy(name = it)
                            errors = errors.copy(nameError = null)
                        },
                        label = { Text("Parent Name") },
                        placeholder = { Text("John Doe") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null)
                        },
                        isError = errors.nameError != null,
                        supportingText = errors.nameError?.let { { Text(it) } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Parent Email Field
                item {
                    OutlinedTextField(
                        value = formState.email,
                        onValueChange = {
                            formState = formState.copy(email = it)
                            errors = errors.copy(emailError = null)
                        },
                        label = { Text("Email") },
                        placeholder = { Text("parent@example.com") },
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

                // Parent Password Field
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

                // Parent Confirm Password Field
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

                // Parent Assign Bus ID Field with Dropdown
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
                    val validationErrors = validateParentForm(formState)
                    if (validationErrors.hasErrors()) {
                        errors = validationErrors
                    } else {
                        val parent = Parent(
                            name = formState.name,
                            email = formState.email,
                            password = formState.password,
                            assignedBusId = formState.assignedBusId,
                            busStopLocation = GeoPoint(
                                formState.latitude.toDouble(),
                                formState.longitude.toDouble()
                            )
                        )
                        onSave(parent)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Primary
                )
            ) {
                if (isLoading) {
                    CircularLoading()
                } else {
                    Text("Add Parent")
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
private fun EditParentDialog(
    isLoading: Boolean,
    showBusDropdown: Boolean,
    parent: Parent,
    onDismiss: () -> Unit,
    onSave: (Parent) -> Unit,
    onDropdownChange: (Boolean) -> Unit,
    availableBusIds: List<String>
) {
    var formState by remember {
        mutableStateOf(
            EditParentFormState(
                name = parent.name,
                assignedBusId = parent.assignedBusId,
                latitude = parent.busStopLocation.latitude.toString(),
                longitude = parent.busStopLocation.longitude.toString()
            )
        )
    }
    var errors by remember { mutableStateOf(EditParentFormErrors()) }

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
                Text("Edit Parent")
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
                        value = parent.email,
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
                        label = { Text("Parent Name") },
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

                // Parent Assign Bus ID Field with Dropdown
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
                    val validationErrors = validateEditParentForm(formState)
                    if (validationErrors.hasErrors()) {
                        errors = validationErrors
                    } else {
                        val updatedParent = parent.copy(
                            name = formState.name,
                            assignedBusId = formState.assignedBusId,
                            busStopLocation = GeoPoint(
                                formState.latitude.toDouble(),
                                formState.longitude.toDouble()
                            )
                        )
                        onSave(updatedParent)
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
private fun validateEditParentForm(state: EditParentFormState): EditParentFormErrors {
    var errors = EditParentFormErrors()

    if (state.name.isBlank()) {
        errors = errors.copy(nameError = "Name is required")
    }

    if (state.assignedBusId.isBlank()) {
        errors = errors.copy(busIdError = "Bus ID is required")
    }

    if (state.latitude.isBlank() || state.longitude.isBlank()) {
        errors = errors.copy(locationError = "Both latitude and longitude are required")
    } else {
        val lat = state.latitude.toDoubleOrNull()
        val lng = state.longitude.toDoubleOrNull()
        if (lat == null || lng == null) {
            errors = errors.copy(locationError = "Invalid coordinates")
        } else if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            errors = errors.copy(locationError = "Coordinates out of range")
        }
    }

    return errors
}

private fun EditParentFormErrors.hasErrors(): Boolean {
    return nameError != null || busIdError != null || locationError != null
}

// Validation
private fun validateParentForm(state: ParentFormState): ParentFormErrors {
    var errors = ParentFormErrors()

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

    if (state.latitude.isBlank() || state.longitude.isBlank()) {
        errors = errors.copy(locationError = "Both latitude and longitude are required")
    } else {
        val lat = state.latitude.toDoubleOrNull()
        val lng = state.longitude.toDoubleOrNull()
        if (lat == null || lng == null) {
            errors = errors.copy(locationError = "Invalid coordinates")
        } else if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            errors = errors.copy(locationError = "Coordinates out of range")
        }
    }

    return errors
}

private fun ParentFormErrors.hasErrors(): Boolean {
    return nameError != null ||
            emailError != null ||
            passwordError != null ||
            confirmPasswordError != null ||
            busIdError != null ||
            locationError != null
}
