package com.ravi.busmanagementt.presentation.home.admin.features.managecaretaker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ravi.busmanagementt.domain.model.Caretaker
import com.ravi.busmanagementt.domain.repository.AdminRepository
import com.ravi.busmanagementt.presentation.home.admin.features.manageparents.AddParentState
import com.ravi.busmanagementt.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CaretakerUiState(
    val isLoading: Boolean = false,
    val caretakers: List<Caretaker> = emptyList(),
    val showDialog: Boolean = false,
    val errorMsg: String? = null,
    val successMsg: String? = null,
)

sealed interface CaretakerEvents {
    data object GetAllCaretakers : CaretakerEvents
    data class OnShowDialog(val shouldShow: Boolean) : CaretakerEvents
    data class AddNewCaretaker(val caretaker: Caretaker) : CaretakerEvents
    data class UpdateCaretaker(val caretaker: Caretaker) : CaretakerEvents
}

@HiltViewModel
class ManageCaretakerViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CaretakerUiState())
    val state = _state.asStateFlow()


    private val _busIds = MutableStateFlow<List<String>>(emptyList())
    val allBusIds = _busIds.asStateFlow()


    init {
        getAllCaretaker()
        getAllBusIds()
    }


    fun onEvent(events: CaretakerEvents) {
        when (events) {
            is CaretakerEvents.AddNewCaretaker -> {
                addNewCaretaker(events.caretaker)
            }

            CaretakerEvents.GetAllCaretakers -> {
                getAllCaretaker()
            }

            is CaretakerEvents.UpdateCaretaker -> {
                updateCaretaker(events.caretaker)
            }

            is CaretakerEvents.OnShowDialog -> {
                _state.update { it.copy(showDialog = events.shouldShow) }
            }
        }
    }


    private fun getAllCaretaker() = viewModelScope.launch(Dispatchers.IO) {
        adminRepository.getAllCaretakers().collect { result ->
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
                            caretakers = result.data ?: emptyList(),
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

    private fun addNewCaretaker(caretaker: Caretaker) = viewModelScope.launch(Dispatchers.IO) {
        adminRepository.addCaretaker(caretaker).collect { result ->
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
                    getAllCaretaker()
                }
            }
        }
    }

    private fun updateCaretaker(caretaker: Caretaker) = viewModelScope.launch(Dispatchers.IO) {
        adminRepository.updateCaretaker(caretaker).collect { result ->
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
                }
            }
        }
    }

}