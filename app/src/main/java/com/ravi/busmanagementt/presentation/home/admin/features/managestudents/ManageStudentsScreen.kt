package com.ravi.busmanagementt.presentation.home.admin.features.managestudents


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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.google.firebase.firestore.GeoPoint
import com.ravi.busmanagementt.domain.model.Attendance
import com.ravi.busmanagementt.domain.model.Ride
import com.ravi.busmanagementt.domain.model.Student
import com.ravi.busmanagementt.presentation.components.CircularLoading
import com.ravi.busmanagementt.presentation.components.NavBackScaffold
import com.ravi.busmanagementt.ui.theme.AppColors
import com.ravi.busmanagementt.utils.showToast


// Form state for adding/editing Student
data class StudentFormState(
    val name: String = "",
    val std: String = "",
    val parentName: String = "",
    val assignedBusId: String = "",
)

data class StudentFormErrors(
    val nameError: String? = null,
    val stdError: String? = null,
    val parentError: String? = null,
    val busIdError: String? = null,
)

// Edit form state (only editable fields)
data class EditStudentFormState(
    val name: String = "",
    val std: String = "",
    val parentName: String = "",
    val assignedBusId: String = "",
)

data class EditStudentFormErrors(
    val nameError: String? = null,
    val stdError: String? = null,
    val parentError: String? = null,
    val busIdError: String? = null
)

// Main Screen
@Composable
fun ManageStudentScreen(
    navController: NavController,
    manageStudentViewModel: ManageStudentsViewModel = hiltViewModel(),
    busId: String? = null
) {

    val context = LocalContext.current
    var editingStudent by remember { mutableStateOf<Student?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Student?>(null) }


    val state by manageStudentViewModel.state.collectAsStateWithLifecycle()
    val availableBusIds by manageStudentViewModel.allBusIds.collectAsStateWithLifecycle()

    var showBusDropdown by remember { mutableStateOf(false) }


    LaunchedEffect(state.isLoading) {
        state.errorMsg?.let { msg ->
            context.showToast(msg)
        }
        state.successMsg?.let { msg ->
            context.showToast(msg)
        }
    }



    ManageStudentContent(
        isFetchingStudents = state.isLoading,
        students = state.students,
        onAddStudent = {
            manageStudentViewModel.onEvent(StudentEvents.OnShowDialog(true))
        },
        onEditStudent = { StudentId ->
            // Navigate to edit screen or show edit dialog
            editingStudent = state.students.find { it.id == StudentId }
            manageStudentViewModel.onEvent(StudentEvents.OnShowDialog(true))
        },
        onDeleteStudent = { Student ->
            showDeleteDialog = Student
        },
        onStudentClick = { StudentId ->
            // Navigate to Student details
        },
        onBackClick = { navController.popBackStack() }
    )



    if (state.showDialog) {
        AddStudentDialog(
            showBusDropdown = showBusDropdown,
            availableBusIds = availableBusIds,
            isLoading = state.isLoading,
            defaultBusId = busId,
            onDismiss = {
                manageStudentViewModel.onEvent(StudentEvents.OnShowDialog(false))
            },
            onSave = { Student ->
                // Create Student
                manageStudentViewModel.onEvent(StudentEvents.AddNewStudent(Student))
            },
            onDropdownChange = { showBusDropdown = it }
        )
    }

    if (state.showDialog){
        editingStudent?.let { student ->
            EditStudentDialog(
                isLoading = state.isLoading,
                showBusDropdown = showBusDropdown,
                student = student,
                onDismiss = { editingStudent = null },
                onSave = { updatedStudent ->
                    // Update Student in firestore database
                    manageStudentViewModel.onEvent(StudentEvents.UpdateStudent(updatedStudent))
                },
                availableBusIds = availableBusIds,
                onDropdownChange = { showBusDropdown = it }
            )
        }
    }


    // Delete Confirmation Dialog
    showDeleteDialog?.let { student ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Student") },
            text = {
                Text("Are you sure you want to delete ${student.name}? This action cannot be undone.")
            },
            confirmButton = {

                Button(
                    onClick = {
//                        manageStudentViewModel.deleteStudent(Student.id, Student.email)
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
private fun ManageStudentContent(
    isFetchingStudents: Boolean,
    students: List<Student>,
    onAddStudent: () -> Unit,
    onEditStudent: (String) -> Unit,
    onDeleteStudent: (Student) -> Unit,
    onStudentClick: (String) -> Unit,
    onBackClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedBusFilter by remember { mutableStateOf("All") }

    val filteredStudents = remember(students, searchQuery, selectedBusFilter) {
        students.filter { student ->
            val matchesSearch = searchQuery.isEmpty() ||
                    student.name.contains(searchQuery, ignoreCase = true) ||
                    student.parentName.contains(searchQuery, ignoreCase = true)

            val matchesBus = selectedBusFilter == "All" ||
                    student.assignedBusId == selectedBusFilter

            matchesSearch && matchesBus
        }
    }

    val availableBuses = remember(students) {
        listOf("All") + students.map { it.assignedBusId }.distinct().sorted()
    }

    NavBackScaffold(
        barTitle = "Manage Students",
        onBackClick = onBackClick,
        fabIcon = Icons.Default.Add,
        onFabClick = onAddStudent
    ) { paddingValues ->

        if (isFetchingStudents) {
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
                StudentsStatsHeader(
                    totalStudents = students.size,
                    filteredStudents = filteredStudents.size
                )

                // Search and Filter
                StudentSearchAndFilter(
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    selectedBus = selectedBusFilter,
                    availableBuses = availableBuses,
                    onBusFilterChange = { selectedBusFilter = it }
                )

                // Students List
                if (filteredStudents.isEmpty()) {
                    EmptyStudentsState(
                        hasSearch = searchQuery.isNotEmpty(),
                        onAddClick = onAddStudent
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = filteredStudents,
                            key = { it.id }
                        ) { filteredStudent ->
                            StudentItem(
                                student = filteredStudent,
                                onClick = { onStudentClick(filteredStudent.id) },
                                onEdit = { onEditStudent(filteredStudent.id) },
                                onDeleteStudent = {
                                    onDeleteStudent(filteredStudent)
                                }
                            )

                            if (filteredStudent.id != filteredStudents.last().id) {
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
private fun StudentsStatsHeader(
    totalStudents: Int,
    filteredStudents: Int
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
                label = "Total Students",
                value = totalStudents.toString(),
                icon = Icons.Default.People,
                modifier = Modifier.weight(1f)
            )

            Spacer(Modifier.width(16.dp))

            StatCard(
                label = "Showing",
                value = filteredStudents.toString(),
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
private fun StudentSearchAndFilter(
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
            placeholder = { Text("Search by name or parent...") },
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
private fun EmptyStudentsState(
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
            text = if (hasSearch) "No Students found" else "No Students added yet",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Gray
        )

        if (!hasSearch) {
            Spacer(Modifier.height(8.dp))

            Text(
                text = "Add Students to assign them to buses",
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
                Text("Add First Student")
            }
        }
    }
}

// Student Item with Attendance
@Composable
fun StudentItem(
    student: Student,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDeleteStudent: (Student) -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    var showAttendance by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Main Student Info
            ListItem(
                modifier = Modifier
                    .clickable(onClick = onClick)
                    .padding(vertical = 4.dp),
                headlineContent = {
                    Text(
                        text = student.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = Color(0xFF1E293B)
                    )
                },
                supportingContent = {
                    Column(modifier = Modifier.padding(top = 4.dp)) {
                        StudentInfoRow(
                            icon = Icons.Default.School,
                            text = student.std,
                            color = Color(0xFF3B82F6)
                        )

                        Spacer(Modifier.height(4.dp))

                        StudentInfoRow(
                            icon = Icons.Default.Person,
                            text = student.parentName,
                            color = Color(0xFF64748B)
                        )

                        Spacer(Modifier.height(4.dp))

                        StudentInfoRow(
                            icon = Icons.Default.DirectionsBus,
                            text = "Bus: ${student.assignedBusId}",
                            color = Color(0xFF8B5CF6)
                        )
                    }
                },
                leadingContent = {
                    StudentAvatar(name = student.name)
                },
                trailingContent = {
                    StudentMenu(
                        showMenu = showMenu,
                        onMenuClick = { showMenu = true },
                        onDismiss = { showMenu = false },
                        onEdit = {
                            showMenu = false
                            onEdit()
                        }
                    )
                }
            )

            // Attendance Section
            if (!student.attendanceList.isNullOrEmpty()) {
                AttendanceSection(
                    attendanceList = student.attendanceList,
                    isExpanded = showAttendance,
                    onToggle = { showAttendance = !showAttendance }
                )
            }
        }
    }
}

@Composable
private fun StudentAvatar(name: String) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(Color(0xFF8B5CF6).copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.take(2).uppercase(),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF8B5CF6)
        )
    }
}

@Composable
private fun StudentInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = color
        )
        Text(
            text = text,
            fontSize = 13.sp,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StudentMenu(
    showMenu: Boolean,
    onMenuClick: () -> Unit,
    onDismiss: () -> Unit,
    onEdit: () -> Unit
) {
    Box {
        IconButton(onClick = onMenuClick) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More options",
                tint = Color(0xFF64748B)
            )
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = onDismiss
        ) {
            DropdownMenuItem(
                text = { Text("Edit") },
                onClick = onEdit,
                leadingIcon = {
                    Icon(Icons.Default.Edit, contentDescription = null)
                }
            )
        }
    }
}

// Attendance Section
@Composable
private fun AttendanceSection(
    attendanceList: List<Attendance>,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = Color(0xFFE2E8F0))

        // Attendance Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.EventNote,
                    contentDescription = "Attendance",
                    modifier = Modifier.size(20.dp),
                    tint = Color(0xFF8B5CF6)
                )
                Text(
                    text = "Attendance Record",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E293B)
                )
                AttendanceBadge(count = attendanceList.size)
            }

            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = Color(0xFF64748B)
            )
        }

        // Attendance List
        if (isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                attendanceList.takeLast(5).reversed().forEach { attendance ->
                    AttendanceCard(attendance = attendance)
                }

                if (attendanceList.size > 5) {
                    Text(
                        text = "Showing last 5 records",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun AttendanceBadge(count: Int) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF8B5CF6).copy(alpha = 0.1f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF8B5CF6),
            fontSize = 11.sp
        )
    }
}

@Composable
private fun AttendanceCard(attendance: Attendance) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Date Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Date",
                        modifier = Modifier.size(14.dp),
                        tint = Color(0xFF64748B)
                    )
                    Text(
                        text = attendance.date,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1E293B)
                    )
                }

                // Day Chip
                DayChip(date = attendance.date)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Pickup & Drop Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RideStatusCard(
                    title = "Pickup",
                    ride = attendance.pickup,
                    icon = Icons.Default.NorthEast,
                    modifier = Modifier.weight(1f)
                )

                RideStatusCard(
                    title = "Drop",
                    ride = attendance.drop,
                    icon = Icons.Default.SouthWest,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun DayChip(date: String) {
    val dayOfWeek = try {
        val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val localDate = java.time.LocalDate.parse(date, formatter)
        localDate.dayOfWeek.getDisplayName(
            java.time.format.TextStyle.SHORT,
            java.util.Locale.getDefault()
        )
    } catch (e: Exception) {
        "N/A"
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF8B5CF6).copy(alpha = 0.1f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = dayOfWeek,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF8B5CF6),
            fontSize = 11.sp
        )
    }
}


@Composable
private fun RideStatusCard(
    title: String,
    ride: Ride?,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    val status = ride?.status ?: "Absent"
    val color = if (status == "Present") Color(0xFF10B981) else Color(0xFFEF4444)

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(6.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 1.dp,
            brush = androidx.compose.ui.graphics.SolidColor(color.copy(alpha = 0.2f))
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(14.dp),
                    tint = color
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B),
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = status,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = color,
                fontSize = 13.sp
            )
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddStudentDialog(
    showBusDropdown: Boolean = false,
    availableBusIds: List<String>,
    isLoading: Boolean = false,
    defaultBusId: String?,
    onDismiss: () -> Unit,
    onSave: (Student) -> Unit,
    onDropdownChange: (Boolean) -> Unit,
) {
    var formState by remember {
        mutableStateOf(
            StudentFormState(assignedBusId = defaultBusId ?: "")
        )
    }
    var errors by remember { mutableStateOf(StudentFormErrors()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Student") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.heightIn(max = 500.dp)
            ) {

                // Student Name Field
                item {
                    OutlinedTextField(
                        value = formState.name,
                        onValueChange = {
                            formState = formState.copy(name = it)
                            errors = errors.copy(nameError = null)
                        },
                        label = { Text("Student Name") },
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

                // Student Standard Field
                item {
                    OutlinedTextField(
                        value = formState.std,
                        onValueChange = {
                            formState = formState.copy(std = it)
                            errors = errors.copy(stdError = null)
                        },
                        label = { Text("Standard") },
                        placeholder = { Text("class 2") },
                        leadingIcon = {
                            Icon(Icons.Default.School, contentDescription = null)
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        isError = errors.stdError != null,
                        supportingText = errors.stdError?.let { { Text(it) } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Student Password Field
                item {
                    OutlinedTextField(
                        value = formState.parentName,
                        onValueChange = {
                            formState = formState.copy(parentName = it)
                            errors = errors.copy(parentError = null)
                        },
                        label = { Text("Parent Name") },
                        leadingIcon = {
                            Icon(Icons.Default.Person3, contentDescription = null)
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        isError = errors.parentError != null,
                        supportingText = errors.parentError?.let { { Text(it) } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Student Assign Bus ID Field with Dropdown
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
                    val validationErrors = validateStudentForm(formState)
                    if (validationErrors.hasErrors()) {
                        errors = validationErrors
                    } else {
                        val Student = Student(
                            name = formState.name,
                            assignedBusId = formState.assignedBusId,
                            id = formState.name,
                            std = formState.std,
                            parentName = formState.parentName
                        )
                        onSave(Student)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Primary
                )
            ) {
                if (isLoading) {
                    CircularLoading()
                } else {
                    Text("Add Student")
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
private fun EditStudentDialog(
    isLoading: Boolean,
    showBusDropdown: Boolean,
    student: Student,
    onDismiss: () -> Unit,
    onSave: (Student) -> Unit,
    onDropdownChange: (Boolean) -> Unit,
    availableBusIds: List<String>
) {
    var formState by remember {
        mutableStateOf(
            EditStudentFormState(
                name = student.name,
                std = student.std,
                parentName = student.parentName,
                assignedBusId = student.assignedBusId,
            )
        )
    }
    var errors by remember { mutableStateOf(EditStudentFormErrors()) }

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
                Text("Edit Student")
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.heightIn(max = 400.dp)
            ) {

                // Editable: Name
                item {
                    OutlinedTextField(
                        value = formState.name,
                        onValueChange = {
                            formState = formState.copy(name = it)
                            errors = errors.copy(nameError = null)
                        },
                        label = { Text("Student Name") },
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
                // Editable: Standard
                item {
                    OutlinedTextField(
                        value = formState.std,
                        onValueChange = {
                            formState = formState.copy(std = it)
                            errors = errors.copy(stdError = null)
                        },
                        label = { Text("Standard") },
                        placeholder = { Text("class 2") },
                        leadingIcon = {
                            Icon(Icons.Default.School, contentDescription = null)
                        },
                        isError = errors.stdError != null,
                        supportingText = errors.stdError?.let { { Text(it) } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                // Editable: Parent Name
                item {
                    OutlinedTextField(
                        value = formState.parentName,
                        onValueChange = {
                            formState = formState.copy(parentName = it)
                            errors = errors.copy(parentError = null)
                        },
                        label = { Text("Parent Name") },
                        placeholder = { Text("Enter parent name") },
                        leadingIcon = {
                            Icon(Icons.Default.Person3, contentDescription = null)
                        },
                        isError = errors.parentError != null,
                        supportingText = errors.parentError?.let { { Text(it) } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Student Assign Bus ID Field with Dropdown
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
                    val validationErrors = validateEditStudentForm(formState)
                    if (validationErrors.hasErrors()) {
                        errors = validationErrors
                    } else {
                        val updatedStudent = student.copy(
                            name = formState.name,
                            std = formState.std,
                            parentName = formState.parentName,
                            assignedBusId = formState.assignedBusId
                        )
                        onSave(updatedStudent)
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
private fun validateEditStudentForm(state: EditStudentFormState): EditStudentFormErrors {
    var errors = EditStudentFormErrors()

    if (state.name.isBlank()) {
        errors = errors.copy(nameError = "Name is required")
    }

    if (state.std.isBlank()) {
        errors = errors.copy(stdError = "Standard is required")
    }

    if (state.parentName.isBlank()) {
        errors = errors.copy(parentError = "Parent name is required")
    }

    if (state.assignedBusId.isBlank()) {
        errors = errors.copy(busIdError = "Bus ID is required")
    }


    return errors
}

private fun EditStudentFormErrors.hasErrors(): Boolean {
    return nameError != null || busIdError != null
}

// Validation
private fun validateStudentForm(state: StudentFormState): StudentFormErrors {
    var errors = StudentFormErrors()

    if (state.name.isBlank()) {
        errors = errors.copy(nameError = "Name is required")
    }

    if (state.std.isBlank()) {
        errors = errors.copy(stdError = "Standard is required")
    }

    if (state.parentName.isBlank()) {
        errors = errors.copy(parentError = "Parent name is required")
    }

    if (state.assignedBusId.isBlank()) {
        errors = errors.copy(busIdError = "Bus ID is required")
    }

    return errors
}

private fun StudentFormErrors.hasErrors(): Boolean {
    return nameError != null ||
            parentError != null ||
            stdError != null ||
            busIdError != null
}
