package com.ravi.busmanagementt.presentation.viewmodels

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ravi.busmanagementt.data.repository.AdminRepositoryImpl
import com.ravi.busmanagementt.domain.model.BusAndDriver
import com.ravi.busmanagementt.domain.repository.AdminRepository
import com.ravi.busmanagementt.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class AddDriverBusViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _addDriverBusState = MutableStateFlow<AddDriverBusState>(AddDriverBusState.Idle)
    val addDriverBusState: StateFlow<AddDriverBusState> = _addDriverBusState.asStateFlow()


    fun addDriverBus(busAndDriver: BusAndDriver) = viewModelScope.launch {
        Log.d("AddDriverBusViewModel", "Entered - Loading state: $busAndDriver")
        adminRepository.addBus(busAndDriver).collect { result ->
            when (result) {
                is Resource.Error -> {
                    Log.d("AddDriverBusViewModel", "Error: ${result.message}")
                    _addDriverBusState.value = AddDriverBusState.Error(result.message ?: "Unknown error")
                }
                is Resource.Loading -> {
                    Log.d("AddDriverBusViewModel", "Loading")
                    _addDriverBusState.value = AddDriverBusState.Loading
                }
                is Resource.Success -> {
                    Log.d("AddDriverBusViewModel", "Success: ${result.data}")
                    _addDriverBusState.value = AddDriverBusState.Success("Bus and Driver added successfully")
                }
            }
        }
    }

}


sealed class AddDriverBusState{
    object Idle : AddDriverBusState()
    data class Success(val message: String) : AddDriverBusState()
    data class Error(val message: String) : AddDriverBusState()
    object Loading : AddDriverBusState()
}