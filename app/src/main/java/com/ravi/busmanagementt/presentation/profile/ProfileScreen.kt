package com.ravi.busmanagementt.presentation.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.ravi.busmanagementt.domain.model.Parent
import com.ravi.busmanagementt.presentation.components.CircularLoading
import com.ravi.busmanagementt.presentation.components.NavBackScaffold
import com.ravi.busmanagementt.presentation.home.BigButton
import com.ravi.busmanagementt.presentation.navigation.NavRoutes
import com.ravi.busmanagementt.presentation.viewmodels.AuthViewModel
import com.ravi.busmanagementt.ui.theme.AppColors

@Composable
fun ProfileScreen(
    navController: NavController,
    authViewModel: AuthViewModel = hiltViewModel(),
    parentProfileViewModel: ParentProfileViewModel = hiltViewModel()
) {

    val context = LocalContext.current

    val parentProfileState by parentProfileViewModel.parentProfileState.collectAsStateWithLifecycle()
    var parentProfile by remember { mutableStateOf<Parent?>(null) }


    LaunchedEffect(parentProfileState) {
        when(val state = parentProfileState){
            is GetParentProfileState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
            }
            GetParentProfileState.Loading -> {}
            is GetParentProfileState.Success -> {
                parentProfile = state.parent
            }
        }
    }

    if(parentProfileState is GetParentProfileState.Loading){
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center){
            CircularLoading(color = AppColors.Primary)
        }
    }else{
        parentProfile?.let {
            ProfileContent(
                parentProfile = it,
                onEditLocationClick = {
                    navController.navigate(NavRoutes.SETTINGS_SCREEN)
                },
                onNavBackClick = {
                    navController.popBackStack()
                },
                logoutClick = {
                    authViewModel.logout()
                    navController.navigate(NavRoutes.LOGIN_SCREEN) {
                        popUpTo(NavRoutes.HOME_SCREEN) {
                            inclusive = true
                        }
                    }
                }
            )
        }

    }
}

@Composable
private fun ProfileContent(
    parentProfile: Parent,
    onNavBackClick: () -> Unit = {},
    onEditLocationClick: () -> Unit = {},
    logoutClick: () -> Unit = {}
) {

    NavBackScaffold(barTitle = "Profile", onBackClick = onNavBackClick) {
        Column(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
                .background(Color.White)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {


            ProfilePhoto()
            Spacer(Modifier.height(36.dp))
            ProfileInfo(parentProfile.email, parentProfile.assignedBusId)
            Spacer(Modifier.height(28.dp))
            StopLocationInfo(stopName = parentProfile.stopName, stopAddress = parentProfile.location, onEditLocationClick = onEditLocationClick)
            Spacer(Modifier.height(28.dp))
            BigButton(
                text = "Log out",
                icon = Icons.Default.Logout
            ) {
                logoutClick()
            }
        }
    }


}

@Composable
private fun ProfilePhoto(modifier: Modifier = Modifier) {
    Box(
        modifier = Modifier
            .size(140.dp)
            .clip(CircleShape)
            .background(Color.LightGray.copy(alpha = .2f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier.size(120.dp),
            imageVector = Icons.Default.Person,
            contentDescription = "",
            tint = AppColors.Primary
        )
    }

}

@Composable
private fun ProfileInfo(
    userEmail: String = "Parent2@gmail.com",
    busId: String = "Bus_4044"
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(userEmail, fontSize = 24.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(12.dp))
        Text("Bus Id: $busId", color = Color.Gray)
    }
}

@Composable
private fun StopLocationInfo(
    stopName: String = "Global School",
    stopAddress: String = "Mumbra devi colony, Diva",
    onEditLocationClick: () -> Unit = {}
) {

    val locationIconColor = remember { Color.Blue }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        onClick = {}
    ) {


        Box(
            Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(
                Modifier.fillMaxWidth(),
            ) {

                // Pickup & Drop Location - Edit - Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color.Blue)
                        )
                        Text(
                            modifier = Modifier.padding(start = 8.dp),
                            text = "Pickup & Drop Location",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                    }
                    TextButton(onClick = onEditLocationClick) {
                        Text("Edit")
                    }
                }



                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Column {


                        Text(
                            modifier = Modifier.padding(top = 12.dp),
                            text = stopName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(stopAddress, color = Color.Gray)

                    }

                    // Location Icon
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(locationIconColor.copy(alpha = .05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "",
                            tint = Color.Blue
                        )
                    }

                }

            }

        }

    }
}










