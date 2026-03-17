package com.ravi.busmanagementt.presentation.home.admin.features.managestudents


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ravi.busmanagementt.domain.model.Student
import com.ravi.busmanagementt.domain.repository.AdminRepository
import com.ravi.busmanagementt.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StudentUiState(
    val isLoading: Boolean = false,
    val students: List<Student> = emptyList(),
    val showDialog: Boolean = false,
    val errorMsg: String? = null,
    val successMsg: String? = null,
)

sealed interface StudentEvents {
    data object GetAllStudents : StudentEvents
    data class OnShowDialog(val shouldShow: Boolean) : StudentEvents
    data class AddNewStudent(val student: Student) : StudentEvents
    data class UpdateStudent(val student: Student) : StudentEvents
}

@HiltViewModel
class ManageStudentsViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _state = MutableStateFlow(StudentUiState())
    val state = _state.asStateFlow()


    private val _busIds = MutableStateFlow<List<String>>(emptyList())
    val allBusIds = _busIds.asStateFlow()


    init {
        getAllStudents()
        getAllBusIds()
    }


    fun onEvent(events: StudentEvents) {
        when (events) {
            is StudentEvents.AddNewStudent -> {
                addNewStudent(events.student)
            }

            StudentEvents.GetAllStudents -> {
                getAllStudents()
            }

            is StudentEvents.UpdateStudent -> {
                updateStudent(events.student)
            }

            is StudentEvents.OnShowDialog -> {
                _state.update { it.copy(showDialog = events.shouldShow) }
            }
        }
    }


    private fun getAllStudents() = viewModelScope.launch(Dispatchers.IO) {
        adminRepository.getAllStudents().collect { result ->
            when (result) {
                is Resource.Error -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            successMsg = null,
                            errorMsg = result.message
                        )
                    }
                }

                is Resource.Loading -> {
                    _state.update { it.copy(isLoading = true) }
                }

                is Resource.Success -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            students = result.data ?: emptyList(),
                            successMsg = result.message,
                            errorMsg = null,
                        )
                    }
                }
            }
        }
    }

    private fun getAllBusIds() = viewModelScope.launch {
        adminRepository.getAllBuses().collect { result ->
            when (result) {
                is Resource.Error -> {

                }

                is Resource.Loading -> {

                }

                is Resource.Success -> {
                    _busIds.value = result.data!!.map { it.busId }
                }
            }
        }
    }

    private fun addNewStudent(student: Student) = viewModelScope.launch(Dispatchers.IO) {
        adminRepository.addStudent(student).collect { result ->
            when (result) {
                is Resource.Error -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMsg = result.message,
                            successMsg = null
                        )
                    }
                }

                is Resource.Loading -> {
                    _state.update { it.copy(isLoading = true) }
                }

                is Resource.Success -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            showDialog = false,
                            errorMsg = null,
                            successMsg = result.message
                        )
                    }
                    getAllStudents()
                }
            }
        }
    }

    private fun updateStudent(student: Student) = viewModelScope.launch(Dispatchers.IO) {
        adminRepository.updateStudent(student).collect { result ->
            when (result) {
                is Resource.Error -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            successMsg = null,
                            errorMsg = result.message
                        )
                    }
                }

                is Resource.Loading -> {
                    _state.update { it.copy(isLoading = true) }
                }

                is Resource.Success -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            showDialog = false,
                            successMsg = result.message,
                            errorMsg = null,
                        )
                    }
                    getAllStudents()
                }
            }
        }
    }

}