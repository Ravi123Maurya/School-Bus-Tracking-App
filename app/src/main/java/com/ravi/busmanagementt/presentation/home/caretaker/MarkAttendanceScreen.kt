package com.ravi.busmanagementt.presentation.home.caretaker


import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// ==================== DATA MODELS ====================

data class Child(
    val id: String,
    val name: String,
    val age: Int,
    val grade: String,
    val parentName: String,
    val parentContact: String,
    val busNumber: String? = null,
    val profileColor: Color = Color(0xFF8B5CF6)
)

data class AttendanceRecord(
    val childId: String,
    val date: LocalDate,
    var status: AttendanceStatus,
    var notes: String = ""
)

enum class AttendanceStatus(val label: String, val color: Color) {
    PRESENT("Present", Color(0xFF10B981)),
    ABSENT("Absent", Color(0xFFEF4444)),
    LATE("Late", Color(0xFFF59E0B)),
    NOT_MARKED("Not Marked", Color(0xFF64748B))
}

// ==================== SAMPLE DATA ====================

object SampleData {
    fun getChildren(): List<Child> = listOf(
        Child("1", "Sumit Verma", 5, "Kindergarten", "Sarah Ali Khan", "+911234567890", "Bus 12", Color(0xFFEC4899)),
        Child("2", "Arin Yadav", 6, "Grade 1", "Aishwarya Rai", "+911234567890", "Bus 12", Color(0xFF3B82F6)),
        Child("3", "Mehta Sharma", 5, "Kindergarten", "Tarak Mehta", "+911234567890", "Bus 15", Color(0xFF8B5CF6)),
        Child("4", "Dipendra Vishwakarma", 7, "Grade 2", "David Goggins", "+911234567890", "Bus 12", Color(0xFF10B981)),
        Child("5", "Sujal Pandey", 6, "Grade 1", "Nirahua Pandey", "+911234567890", "Bus 15", Color(0xFFF59E0B)),
        Child("6", "Ramesh Patel", 5, "Kindergarten", "Guddu Patel", "+911234567890", "Bus 12", Color(0xFF06B6D4))
    )
}

// ==================== MARK ATTENDANCE SCREEN ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkAttendanceScreen(
    navController: NavController,
    children: List<Child> = SampleData.getChildren()
) {
    val today = remember { LocalDate.now() }
    val attendanceMap = remember {
        mutableStateMapOf<String, AttendanceStatus>().apply {
            children.forEach { child ->
                this[child.id] = AttendanceStatus.NOT_MARKED
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mark Attendance") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF1E293B)
                )
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Date Header
            AttendanceDateHeader(date = today)

            // Summary Stats
            AttendanceSummary(attendanceMap = attendanceMap)

            Spacer(modifier = Modifier.height(16.dp))

            // Children List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(children) { child ->
                    AttendanceCard(
                        child = child,
                        status = attendanceMap[child.id] ?: AttendanceStatus.NOT_MARKED,
                        onStatusChange = { newStatus ->
                            attendanceMap[child.id] = newStatus
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }

        // Submit Button
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.BottomCenter
        ) {
            SubmitAttendanceButton(
                enabled = attendanceMap.values.none { it == AttendanceStatus.NOT_MARKED },
                onSubmit = {
                    // Handle submit
                    navController.navigateUp()
                }
            )
        }
    }
}

@Composable
private fun AttendanceDateHeader(date: LocalDate) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = "Date",
                tint = Color(0xFFEC4899),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = date.format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy")),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = "Mark today's attendance",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}

@Composable
private fun AttendanceSummary(attendanceMap: Map<String, AttendanceStatus>) {
    val present = attendanceMap.values.count { it == AttendanceStatus.PRESENT }
    val absent = attendanceMap.values.count { it == AttendanceStatus.ABSENT }
    val late = attendanceMap.values.count { it == AttendanceStatus.LATE }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SummaryCard("Present", present, Color(0xFF10B981), Modifier.weight(1f))
        SummaryCard("Absent", absent, Color(0xFFEF4444), Modifier.weight(1f))
        SummaryCard("Late", late, Color(0xFFF59E0B), Modifier.weight(1f))
    }
}

@Composable
private fun SummaryCard(label: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF64748B)
            )
        }
    }
}

@Composable
private fun AttendanceCard(
    child: Child,
    status: AttendanceStatus,
    onStatusChange: (AttendanceStatus) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(child.profileColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = child.name.first().toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = child.profileColor
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Child Info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = child.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = child.grade,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                }

                // Status Chip
                StatusChip(status = status, onClick = { expanded = !expanded })
            }

            // Status Options
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = Color(0xFFE2E8F0))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AttendanceStatus.values().filterNot { it == AttendanceStatus.NOT_MARKED }.forEach { statusOption ->
                        StatusButton(
                            status = statusOption,
                            isSelected = status == statusOption,
                            onClick = {
                                onStatusChange(statusOption)
                                expanded = false
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: AttendanceStatus, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(status.color.copy(alpha = 0.1f))
            .border(1.dp, status.color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = status.label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = status.color
            )
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = "Expand",
                modifier = Modifier.size(16.dp),
                tint = status.color
            )
        }
    }
}

@Composable
private fun StatusButton(
    status: AttendanceStatus,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) status.color else status.color.copy(alpha = 0.1f),
            contentColor = if (isSelected) Color.White else status.color
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = status.label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SubmitAttendanceButton(enabled: Boolean, onSubmit: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(48.dp),
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFEC4899),
                disabledContainerColor = Color(0xFFE2E8F0)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Submit Attendance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ==================== VIEW CHILDREN SCREEN ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewChildrenScreen(
    navController: NavController,
    children: List<Child> = SampleData.getChildren()
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredChildren = remember(searchQuery, children) {
        if (searchQuery.isBlank()) {
            children
        } else {
            children.filter { child ->
                child.name.contains(searchQuery, ignoreCase = true) ||
                        child.grade.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("View Children") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF1E293B)
                )
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                modifier = Modifier.padding(20.dp)
            )

            // Children Count
            Text(
                text = "${filteredChildren.size} Children",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            // Children List
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredChildren) { child ->
                    ChildCard(
                        child = child,
                        onClick = {
                            // Navigate to child details
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text("Search by name or grade...") },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = "Search")
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                }
            }
        },
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedBorderColor = Color(0xFF8B5CF6),
            unfocusedBorderColor = Color(0xFFE2E8F0)
        ),
        singleLine = true
    )
}

@Composable
private fun ChildCard(child: Child, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(child.profileColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = child.name.split(" ").map { it.first() }.joinToString(""),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = child.profileColor
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Child Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = child.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    InfoChip(icon = Icons.Default.School, text = child.grade)
                    InfoChip(icon = Icons.Default.Person, text = "${child.age} yrs")
                    child.busNumber?.let {
                        InfoChip(icon = Icons.Default.DirectionsBus, text = it)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Parent: ${child.parentName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Arrow
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View Details",
                tint = Color(0xFFCBD5E1),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun InfoChip(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFFF1F5F9))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = Color(0xFF64748B)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 11.sp,
            color = Color(0xFF64748B),
            fontWeight = FontWeight.Medium
        )
    }
}

// ==================== PREVIEWS ====================

@Preview(showBackground = true)
@Composable
private fun AttendanceCardPreview() {
    MaterialTheme {
        AttendanceCard(
            child = SampleData.getChildren().first(),
            status = AttendanceStatus.NOT_MARKED,
            onStatusChange = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ChildCardPreview() {
    MaterialTheme {
        ChildCard(
            child = SampleData.getChildren().first(),
            onClick = {}
        )
    }
}