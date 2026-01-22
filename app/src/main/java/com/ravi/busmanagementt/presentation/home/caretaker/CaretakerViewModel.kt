package com.ravi.busmanagementt.presentation.home.caretaker

import android.util.Log
import androidx.core.os.registerForAllProfilingResults
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ravi.busmanagementt.domain.repository.CaretakerRepository
import com.ravi.busmanagementt.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


data class UiState(
    val students: List<Child> = emptyList(),
    val isLoading: Boolean = true,
    val successMsg: String? = null,
    val errorMsg: String? = null,
)

sealed interface UiEvent {
    data object GetStudents : UiEvent
    data class MarkAttendance(val studId: String, val status: String) : UiEvent
}

@HiltViewModel
class CaretakerViewModel @Inject constructor(
    private val caretakerRepository: CaretakerRepository
) : ViewModel() {


    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()


    init {
        getStudents()
    }


    fun onEvent(uiEvent: UiEvent){
        when(uiEvent){
            is UiEvent.GetStudents -> {
                getStudents()
            }

            is UiEvent.MarkAttendance -> {
                markAttendance(uiEvent.studId, uiEvent.status)
            }
        }
    }


    private fun getStudents() {
        viewModelScope.launch(Dispatchers.IO) {
            caretakerRepository.getStudentsOfBus().collect { result ->
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
        Log.d("ATTEND", "VM - entered")
        viewModelScope.launch(Dispatchers.IO) {
            caretakerRepository.markAttendance(stuId, status).collect { result ->
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
                        Log.d("ATTEND", "VM - result - ${result.message}")
                    }
                }
            }
        }
    }


}