package com.ravi.busmanagementt.presentation.viewmodels

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.ravi.busmanagementt.data.datastore.PortalManager
import com.ravi.busmanagementt.data.datastore.UserPrefManager
import com.ravi.busmanagementt.data.serivce.LocationService
import com.ravi.busmanagementt.domain.repository.AuthRepository
import com.ravi.busmanagementt.domain.repository.UserRepository
import com.ravi.busmanagementt.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class AuthViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
    private val firebaseAuth: FirebaseAuth,
    private val userPrefManager: UserPrefManager,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _isLoggedIn = MutableStateFlow(firebaseAuth.currentUser != null)
    val isLoggedIn = _isLoggedIn.asStateFlow()

    private val _loginState = MutableStateFlow<AuthState>(AuthState.Idle)
    var loginState = _loginState.asStateFlow()

    private val authStateListener = FirebaseAuth.AuthStateListener { auth ->
        _isLoggedIn.value = auth.currentUser != null
    }

    val email = firebaseAuth.currentUser?.email

    init {
        firebaseAuth.addAuthStateListener(authStateListener)
    }

    fun login(email: String, password: String) = viewModelScope.launch {
        authRepository.login(email, password).collect { result ->
            when (result) {
                is Resource.Success -> {
                    Log.d("AuthViewModel", "Calling setFCMToken")
                    setFcmToken()
                    _loginState.value = AuthState.Success("Login successful")
                }

                is Resource.Error -> {
                    _loginState.value = AuthState.Error("Login failed, Account doesn't exist")
                }

                is Resource.Loading -> {
                    _loginState.value = AuthState.Loading
                }
            }
        }

    }

    private fun setFcmToken() {
        Log.d("AuthViewModel", " SetFCMToken Called")
        var token: String? = null
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            Log.d("AuthViewModel", " SetFCMToken Toke: ${task.result}")
            if (!task.isSuccessful) {
                return@addOnCompleteListener
            } else {
                token = task.result
                viewModelScope.launch {  userRepository.setFcmToken(token) }
            }
        }
    }


    fun logout() = viewModelScope.launch {
        val serviceIntent = Intent(context, LocationService::class.java).apply {
            action = LocationService.ACTION_STOP
        }
        context.startService(serviceIntent)
        userPrefManager.setBusId(null)
        PortalManager.setParentBusStopLocation(context, null)
        authRepository.logout()
    }

    override fun onCleared() {
        super.onCleared()
        firebaseAuth.removeAuthStateListener(authStateListener)
    }

}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Error(val message: String) : AuthState()
    data class Success(val message: String) : AuthState()
}