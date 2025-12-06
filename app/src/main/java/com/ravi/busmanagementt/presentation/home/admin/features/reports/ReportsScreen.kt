package com.ravi.busmanagementt.presentation.home.admin.features.reports

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ravi.busmanagementt.presentation.components.NavBackScaffold
import com.ravi.busmanagementt.ui.theme.AppColors
import java.text.SimpleDateFormat
import java.util.*

// Data Models
data class ReportSummary(
    val totalBuses: Int,
    val activeBuses: Int,
    val totalParents: Int,
    val totalDrivers: Int,
    val totalRoutes: Int,
    val onTimePercentage: Float
)

data class BusPerformance(
    val busId: String,
    val busName: String,
    val completedTrips: Int,
    val onTimeTrips: Int,
    val delayedTrips: Int,
    val averageDelay: Int // in minutes
)

data class RouteStats(
    val routeName: String,
    val totalStops: Int,
    val averageTime: Int, // in minutes
    val popularityScore: Float // 0-100
)

data class AttendanceRecord(
    val date: String,
    val presentCount: Int,
    val absentCount: Int,
    val totalStudents: Int
)

enum class ReportPeriod(val displayName: String) {
    TODAY("Today"),
    WEEK("This Week"),
    MONTH("This Month"),
    YEAR("This Year"),
    CUSTOM("Custom Range")
}

// Main Screen
@Composable
fun ReportsScreen(navController: NavController) {
    ReportsContent(
        onBackClick = { navController.popBackStack() }
    )
}

@Composable
private fun ReportsContent(onBackClick: () -> Unit) {
    var selectedPeriod by remember { mutableStateOf(ReportPeriod.WEEK) }
    var expandedSection by remember { mutableStateOf<String?>("overview") }

    // Sample data - In real app, fetch from ViewModel
    val reportSummary = remember { getSampleReportSummary() }
    val busPerformance = remember { getSampleBusPerformance() }
    val routeStats = remember { getSampleRouteStats() }
    val attendanceRecords = remember { getSampleAttendanceRecords() }

    NavBackScaffold(
        barTitle = "Reports & Analytics",
        onBackClick = onBackClick
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {


            item {
                Text(
                    text = "Future Development",
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Blue.copy(alpha = 0.1f))
                        .padding(12.dp, 8.dp),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Blue,
                    textAlign = TextAlign.Center
                )
            }

            // Period Selector
            item {
                PeriodSelector(
                    selectedPeriod = selectedPeriod,
                    onPeriodChange = { selectedPeriod = it }
                )
            }

            // Overview Section
            item {
                ExpandableSection(
                    title = "Overview",
                    icon = Icons.Default.Dashboard,
                    isExpanded = expandedSection == "overview",
                    onToggle = {
                        expandedSection = if (expandedSection == "overview") null else "overview"
                    }
                ) {
                    OverviewSection(summary = reportSummary)
                }
            }

            // Bus Performance Section
            item {
                ExpandableSection(
                    title = "Bus Performance",
                    icon = Icons.Default.DirectionsBus,
                    isExpanded = expandedSection == "bus_performance",
                    onToggle = {
                        expandedSection =
                            if (expandedSection == "bus_performance") null else "bus_performance"
                    }
                ) {
                    BusPerformanceSection(performances = busPerformance)
                }
            }

            // Route Analytics Section
            item {
                ExpandableSection(
                    title = "Route Analytics",
                    icon = Icons.Default.Route,
                    isExpanded = expandedSection == "route_analytics",
                    onToggle = {
                        expandedSection =
                            if (expandedSection == "route_analytics") null else "route_analytics"
                    }
                ) {
                    RouteAnalyticsSection(routes = routeStats)
                }
            }

            // Attendance Tracking Section
            item {
                ExpandableSection(
                    title = "Attendance Tracking",
                    icon = Icons.Default.PersonAdd,
                    isExpanded = expandedSection == "attendance",
                    onToggle = {
                        expandedSection =
                            if (expandedSection == "attendance") null else "attendance"
                    }
                ) {
                    AttendanceSection(records = attendanceRecords)
                }
            }

            // Export Options
            item {
                ExportSection()
            }
        }
    }
}

@Composable
private fun PeriodSelector(
    selectedPeriod: ReportPeriod,
    onPeriodChange: (ReportPeriod) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = null,
                tint = AppColors.Primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Report Period",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray
            )
        }

        Spacer(Modifier.height(12.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ReportPeriod.values()) { period ->
                FilterChip(
                    selected = selectedPeriod == period,
                    onClick = { onPeriodChange(period) },
                    label = { Text(period.displayName) },
                    leadingIcon = if (selectedPeriod == period) {
                        {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else null
                )
            }
        }
    }
}

@Composable
private fun ExpandableSection(
    title: String,
    icon: ImageVector,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(AppColors.Primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = AppColors.Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = Color.Gray
                )
            }

            // Content
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                    Spacer(Modifier.height(16.dp))
                    content()
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun OverviewSection(summary: ReportSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Key Metrics Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                label = "Total Buses",
                value = summary.totalBuses.toString(),
                icon = Icons.Default.DirectionsBus,
                color = AppColors.Primary,
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                label = "Active",
                value = summary.activeBuses.toString(),
                icon = Icons.Default.CheckCircle,
                color = Color(0xFF4CAF50),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                label = "Parents",
                value = summary.totalParents.toString(),
                icon = Icons.Default.People,
                color = Color(0xFF2196F3),
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                label = "Drivers",
                value = summary.totalDrivers.toString(),
                icon = Icons.Default.Person,
                color = Color(0xFFFF9800),
                modifier = Modifier.weight(1f)
            )
        }

        // On-Time Performance
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = AppColors.Primary.copy(alpha = 0.05f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "On-Time Performance",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray
                    )

                    Text(
                        text = "${(summary.onTimePercentage * 100).toInt()}%",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (summary.onTimePercentage >= 0.8f) Color(0xFF4CAF50) else Color(
                            0xFFFF9800
                        )
                    )
                }

                Spacer(Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { summary.onTimePercentage },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (summary.onTimePercentage >= 0.8f) Color(0xFF4CAF50) else Color(
                        0xFFFF9800
                    ),
                    trackColor = Color.LightGray.copy(alpha = 0.3f),
                    strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                )
            }
        }
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )

            Text(
                text = label,
                fontSize = 12.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun BusPerformanceSection(performances: List<BusPerformance>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        performances.forEach { performance ->
            BusPerformanceCard(performance = performance)
        }
    }
}

@Composable
private fun BusPerformanceCard(performance: BusPerformance) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = performance.busName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = performance.busId,
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }

                val onTimeRate = if (performance.completedTrips > 0) {
                    (performance.onTimeTrips.toFloat() / performance.completedTrips * 100).toInt()
                } else 0

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (onTimeRate >= 80) Color(0xFF4CAF50) else Color(0xFFFF9800)
                ) {
                    Text(
                        text = "$onTimeRate%",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PerformanceMetric(
                    label = "Total Trips",
                    value = performance.completedTrips.toString(),
                    icon = Icons.Default.CheckCircle,
                    color = AppColors.Primary
                )

                PerformanceMetric(
                    label = "On Time",
                    value = performance.onTimeTrips.toString(),
                    icon = Icons.Default.Check,
                    color = Color(0xFF4CAF50)
                )

                PerformanceMetric(
                    label = "Delayed",
                    value = performance.delayedTrips.toString(),
                    icon = Icons.Default.Warning,
                    color = Color(0xFFFF9800)
                )
            }

            if (performance.averageDelay > 0) {
                Spacer(Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color.Gray
                    )
                    Text(
                        text = "Avg Delay: ${performance.averageDelay} min",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
private fun PerformanceMetric(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )

        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )

        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.Gray
        )
    }
}

@Composable
private fun RouteAnalyticsSection(routes: List<RouteStats>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        routes.forEach { route ->
            RouteStatsCard(route = route)
        }
    }
}

@Composable
private fun RouteStatsCard(route: RouteStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = route.routeName,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "${route.totalStops}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Primary
                    )
                    Text(
                        text = "Total Stops",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                Column {
                    Text(
                        text = "${route.averageTime} min",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2196F3)
                    )
                    Text(
                        text = "Avg Time",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                Column {
                    Text(
                        text = "${route.popularityScore.toInt()}%",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                    Text(
                        text = "Popularity",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
private fun AttendanceSection(records: List<AttendanceRecord>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        records.forEach { record ->
            AttendanceCard(record = record)
        }
    }
}

@Composable
private fun AttendanceCard(record: AttendanceRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = record.date,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Gray
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Present
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF4CAF50).copy(alpha = 0.1f))
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = record.presentCount.toString(),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                        Text(
                            text = "Present",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                // Absent
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE53935).copy(alpha = 0.1f))
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = record.absentCount.toString(),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE53935)
                        )
                        Text(
                            text = "Absent",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            val attendanceRate =
                (record.presentCount.toFloat() / record.totalStudents * 100).toInt()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Attendance Rate",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Text(
                    text = "$attendanceRate%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (attendanceRate >= 80) Color(0xFF4CAF50) else Color(0xFFFF9800)
                )
            }

            LinearProgressIndicator(
                progress = attendanceRate / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (attendanceRate >= 80) Color(0xFF4CAF50) else Color(0xFFFF9800),
                trackColor = Color.LightGray.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
private fun ExportSection() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = null,
                    tint = AppColors.Primary,
                    modifier = Modifier.size(24.dp)
                )

                Text(
                    text = "Export Reports",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { /* TODO: Export as PDF */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("PDF")
                }

                OutlinedButton(
                    onClick = { /* TODO: Export as Excel */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.TableChart,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Excel")
                }
            }
        }
    }
}

// Sample Data Functions
private fun getSampleReportSummary() = ReportSummary(
    totalBuses = 12,
    activeBuses = 10,
    totalParents = 145,
    totalDrivers = 12,
    totalRoutes = 8,
    onTimePercentage = 0.85f
)

private fun getSampleBusPerformance() = listOf(
    BusPerformance("BUS-001", "Manisha", 45, 38, 7, 5),
    BusPerformance("BUS-002", "Rajesh", 42, 40, 2, 2),
    BusPerformance("BUS-003", "Krishna", 38, 30, 8, 8),
    BusPerformance("BUS-004", "Lakshmi", 50, 48, 2, 1)
)

private fun getSampleRouteStats() = listOf(
    RouteStats("Downtown Route", 12, 45, 85f),
    RouteStats("Suburban Route", 8, 35, 72f),
    RouteStats("Express Route", 6, 25, 90f),
    RouteStats("Extended Route", 15, 60, 65f)
)

private fun getSampleAttendanceRecords() = listOf(
    AttendanceRecord("Today - Dec 04, 2025", 138, 7, 145),
    AttendanceRecord("Yesterday - Dec 03, 2025", 142, 3, 145),
    AttendanceRecord("Dec 02, 2025", 135, 10, 145),
    AttendanceRecord("Dec 01, 2025", 140, 5, 145)
)