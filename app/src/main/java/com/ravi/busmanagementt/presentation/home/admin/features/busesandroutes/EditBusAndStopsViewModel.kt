package com.ravi.busmanagementt.presentation.home.admin.features.busesandroutes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ravi.busmanagementt.domain.model.BusAndDriver
import com.ravi.busmanagementt.domain.repository.AdminRepository
import com.ravi.busmanagementt.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class EditBusAndStopsViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {


    private val _busDataState = MutableStateFlow<GetBusDataState>(GetBusDataState.Loading)
    val busDataState = _busDataState.asStateFlow()

    private val _updateBusDataState = MutableStateFlow<UpdateBusDataState>(UpdateBusDataState.Idle)
    val updateBusDataState = _updateBusDataState.asStateFlow()

    private val _deleteBusStopState = MutableStateFlow<DeleteBusStopState>(DeleteBusStopState.Idle)
    val deleteBusStopState = _deleteBusStopState.asStateFlow()


    fun getBusData(busId: String) {
        viewModelScope.launch {
            adminRepository.getBusWithId(busId).collect { result ->
                when (result) {
                    is Resource.Error -> {
                        _busDataState.value =
                            GetBusDataState.Error(result.message ?: "Unknown error")
                    }

                    is Resource.Loading -> {
                        _busDataState.value = GetBusDataState.Loading
                    }

                    is Resource.Success -> {
                        val busData = result.data as BusAndDriver
                        _busDataState.value = GetBusDataState.Success(busData)
                    }
                }
            }
        }
    }

    fun updateBusData(busData: BusAndDriver) {
        viewModelScope.launch {
            adminRepository.updateBus(busData).collect { result ->
                when(result){
                    is Resource.Error -> {
                        _updateBusDataState.value = UpdateBusDataState.Error(result.message ?: "Unknown error")
                    }
                    is Resource.Loading -> {
                        _updateBusDataState.value = UpdateBusDataState.Loading
                    }
                    is Resource.Success -> {
                        _updateBusDataState.value = UpdateBusDataState.Success(result.data ?: "Unknown error")
                    }
                }
            }
        }
    }

    fun deleteBusStop(busId: String, stopIndex: Int) {

    }


    fun resetAllStates(){
        _busDataState.value = GetBusDataState.Idle
        _updateBusDataState.value = UpdateBusDataState.Idle
        _deleteBusStopState.value = DeleteBusStopState.Idle
    }
}

sealed class GetBusDataState {
    object Idle: GetBusDataState()
    object Loading : GetBusDataState()
    data class Error(val message: String) : GetBusDataState()
    data class Success(val busData: BusAndDriver) : GetBusDataState()
}

sealed class UpdateBusDataState {
    object Idle : UpdateBusDataState()
    object Loading : UpdateBusDataState()
    data class Error(val message: String) : UpdateBusDataState()
    data class Success(val message: String) : UpdateBusDataState()
}

sealed class DeleteBusStopState {
    object Idle : DeleteBusStopState()
    object Loading : DeleteBusStopState()
    data class Error(val message: String) : DeleteBusStopState()
    data class Success(val message: String) : DeleteBusStopState()
}