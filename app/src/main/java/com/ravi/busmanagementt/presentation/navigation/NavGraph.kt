package com.ravi.busmanagementt.presentation.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.ravi.busmanagementt.presentation.auth.LoginScreen
import com.ravi.busmanagementt.presentation.auth.AccountTypeScreen
import com.ravi.busmanagementt.presentation.home.HomeScreen
import com.ravi.busmanagementt.presentation.home.admin.features.addriverbus.AddDriverBusScreen
import com.ravi.busmanagementt.presentation.home.admin.features.allbuses.AllBusesScreen
import com.ravi.busmanagementt.presentation.home.admin.features.allbuses.BusStopsScreen
import com.ravi.busmanagementt.presentation.home.admin.features.manageparents.ManageParentsScreen
import com.ravi.busmanagementt.presentation.home.admin.features.reports.ReportsScreen
import com.ravi.busmanagementt.presentation.onboarding.OnboardingScreen
import com.ravi.busmanagementt.presentation.profile.ProfileScreen
import com.ravi.busmanagementt.presentation.settings.SettingsScreen
import com.ravi.busmanagementt.presentation.viewmodels.AuthViewModel
import com.ravi.busmanagementt.presentation.viewmodels.MapViewModel
import kotlinx.serialization.Serializable


@Composable
fun NavGraph(
    mapViewModel: MapViewModel
) {

    val authViewModel: AuthViewModel = hiltViewModel()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()

    if (isLoggedIn) {
        MainNavGraph(mapViewModel)
    } else {
        AuthNavGraph()
    }

}

@Composable
fun AuthNavGraph() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = NavRoutes.ONBOARDING_SCREEN
    ) {

        composable(NavRoutes.ONBOARDING_SCREEN) {
            OnboardingScreen(navController) {
                navController.navigate(NavRoutes.ACC_TYPE_SCREEN) {
                    popUpTo(NavRoutes.ONBOARDING_SCREEN) {
                        inclusive = true
                    }
                }
            }
        }

        composable(NavRoutes.ACC_TYPE_SCREEN) {
            AccountTypeScreen(navController)
        }
        composable(NavRoutes.LOGIN_SCREEN) {
            LoginScreen(navController)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MainNavGraph(
    mapViewModel: MapViewModel
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = HomeScreen::class
    ) {
        composable<HomeScreen> {
            val args = it.toRoute<HomeScreen>()
            HomeScreen(navController, mapViewModel = mapViewModel, newBusId = args.busId)
        }

        composable(
            route = NavRoutes.SETTINGS_SCREEN,
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() }
        ) {
            SettingsScreen(navController)
        }

        //Parent
        composable(NavRoutes.PROFILE_SCREEN) {
            ProfileScreen(navController)
        }

        //----------- Admin ---------//
        composable(NavRoutes.ALL_BUSES_SCREEN) {
            AllBusesScreen(navController)
        }
        composable(NavRoutes.ADD_DRIVER_BUS_SCREEN) {
            AddDriverBusScreen(navController)
        }

        composable(NavRoutes.MANAGE_PARENT_SCREEN) {
            ManageParentsScreen(navController)
        }

        composable(NavRoutes.REPORTS_SCREEN) {
            ReportsScreen(navController)
        }

        composable<BusStopsScreen> {
            val args = it.toRoute<BusStopsScreen>()
            BusStopsScreen(navController, mapViewModel, busId = args.busId)
        }

    }
}


@Serializable
data class HomeScreen(
    val busId: String? = null
)

@Serializable
data class BusStopsScreen(
    val busId: String? = null
)