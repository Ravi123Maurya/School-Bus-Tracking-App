package com.ravi.busmanagementt.presentation.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.ravi.busmanagementt.data.datastore.PortalManager
import com.ravi.busmanagementt.data.datastore.Portals
import com.ravi.busmanagementt.data.datastore.UserPrefManager
import com.ravi.busmanagementt.domain.model.BusStop
import com.ravi.busmanagementt.domain.repository.FirestoreBusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class BusViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestoreBusRepository: FirestoreBusRepository,
    private val userPrefManager: UserPrefManager,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _busId = MutableStateFlow<String?>(null)
    val busId = _busId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val busStops: StateFlow<List<BusStop>> = _busId.flatMapLatest { busId ->
        if (busId != null) {
            firestoreBusRepository.getBusRouteStops(busId)
        } else {
            MutableStateFlow(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    init {
        ensureBusIdExists()
    }

    private fun ensureBusIdExists() = viewModelScope.launch {
        var currentBusId = userPrefManager.getBusId().firstOrNull()
        if (currentBusId == null) {
            val email = firebaseAuth.currentUser?.email
            if (email != null) {
                currentBusId =
                    if (PortalManager.getPortal(context).firstOrNull() == Portals.DRIVER.value)
                        firestoreBusRepository.getBusIdForDriverFromEmail(email)
                    else firestoreBusRepository.getBusIdForParentFromEmail(email)

                userPrefManager.setBusId(currentBusId)
            }
        }
        if (currentBusId != null) {
            _busId.value = currentBusId
        } else {
            _busId.value = null
        }
    }


}