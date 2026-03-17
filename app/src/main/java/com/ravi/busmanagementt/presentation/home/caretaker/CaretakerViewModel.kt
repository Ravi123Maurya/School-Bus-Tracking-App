package com.ravi.busmanagementt.presentation.home.caretaker

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ravi.busmanagementt.data.datastore.UserPrefManager
import com.ravi.busmanagementt.domain.repository.CaretakerRepository
import com.ravi.busmanagementt.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject


data class UiState(
    val students: List<Child> = emptyList(),
    val isLoading: Boolean = true,
    val successMsg: String? = null,
    val errorMsg: String? = null,
)

sealed interface UiEvent {
    data object GetStudents : UiEvent
    data class OnRideTypeClick(val isPickup: Boolean) : UiEvent
    data class MarkAttendance(val studId: String, val status: String) : UiEvent
}

@HiltViewModel
class CaretakerViewModel @Inject constructor(
    private val caretakerRepository: CaretakerRepository,
    private val userPrefManager: UserPrefManager
) : ViewModel() {


    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    val isPickupRide = userPrefManager.getRideType()
        .map { it }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            null
        )

    init {
        getStudents()
        resetRideTypeOnNewDay()
    }

    fun onEvent(uiEvent: UiEvent) {
        when (uiEvent) {
            is UiEvent.GetStudents -> {
                getStudents()
            }

            is UiEvent.OnRideTypeClick -> {
                setRideType(uiEvent.isPickup)
            }

            is UiEvent.MarkAttendance -> {
                markAttendance(uiEvent.studId, uiEvent.status)
            }
        }
    }

    private fun setRideType(isPickup: Boolean?) = viewModelScope.launch {
        userPrefManager.setCurrentAttendanceDate(LocalDate.now().toString())
        userPrefManager.setRideType(isPickup)
    }

    private fun getStudents() {
        viewModelScope.launch(Dispatchers.IO) {
            caretakerRepository.getStudentsForCaretaker().collect { result ->
                when (result) {
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                successMsg = null,
                                errorMsg = result.message
                            )
                        }
                    }

                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }

                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMsg = null,
                                successMsg = result.message,
                                students = result.data ?: emptyList()
                            )
                        }
                    }
                }
            }
        }
    }

    private fun markAttendance(stuId: String, status: String) {
        viewModelScope.launch(Dispatchers.IO) {
            caretakerRepository.markAttendance(stuId, status, isPickupRide.value)
                .collect { result ->
                    when (result) {
                        is Resource.Error -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    successMsg = null,
                                    errorMsg = result.message
                                )
                            }
                            Log.d("ATTEND", "VM - result - ${result.message}")
                        }

                        is Resource.Loading -> {
                            _uiState.update { it.copy(isLoading = true) }
                        }

                        is Resource.Success -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMsg = null,
                                    successMsg = result.message,
                                )
                            }
                        }
                    }
                }
        }
    }


    private fun resetRideTypeOnNewDay() = viewModelScope.launch {
        if(userPrefManager.getCurrentAttendanceDate().firstOrNull() != LocalDate.now().toString()){
            setRideType(null)
        }
    }
}