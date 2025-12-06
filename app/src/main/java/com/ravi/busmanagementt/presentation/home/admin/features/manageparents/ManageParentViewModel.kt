package com.ravi.busmanagementt.presentation.home.admin.features.manageparents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ravi.busmanagementt.domain.model.Parent
import com.ravi.busmanagementt.domain.repository.AdminRepository
import com.ravi.busmanagementt.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class ManageParentViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _addParentState = MutableStateFlow<AddParentState>(AddParentState.Idle)
    val addParentState = _addParentState.asStateFlow()

    private val _busIds = MutableStateFlow<List<String>>(emptyList())
    val allBusIds = _busIds.asStateFlow()

    private val _getParentState = MutableStateFlow<GetParentState>(GetParentState.Loading)
    val getParentState = _getParentState.asStateFlow()

    private val _updateParentState = MutableStateFlow<UpdateParentState>(UpdateParentState.Idle)
    val updateParentState = _updateParentState.asStateFlow()

    private val _deleteParentState = MutableStateFlow<DeleteParentState>(DeleteParentState.Idle)
    val deleteParentState = _deleteParentState.asStateFlow()


    init {
        getAllParents()
        getAllBusIds()
    }

    fun addNewParent(parent: Parent) = viewModelScope.launch {
        adminRepository.addParent(parent).collect { result ->
            when (result) {
                is Resource.Error -> {
                    _addParentState.value = AddParentState.Error(result.message ?: "Unknown error")
                }

                is Resource.Loading -> {
                    _addParentState.value = AddParentState.Loading
                }

                is Resource.Success -> {
                    _addParentState.value =
                        AddParentState.Success(result.data ?: "Parent added successfully")
                }
            }
        }
    }

    fun getAllParents() = viewModelScope.launch {
        adminRepository.getAllParents().collect { result ->
            when (result) {
                is Resource.Error -> {
                    _getParentState.value = GetParentState.Error(result.message!!)
                }

                is Resource.Loading -> {
                    _getParentState.value = GetParentState.Loading
                }

                is Resource.Success -> {
                    _getParentState.value = GetParentState.Success(result.data!!)
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

    fun updateParent(parent: Parent) = viewModelScope.launch {
        adminRepository.updateParent(parent).collect { result ->
            when(result){
                is Resource.Error -> {
                    _updateParentState.value = UpdateParentState.Error(result.message!!)
                }
                is Resource.Loading -> {
                    _updateParentState.value = UpdateParentState.Loading
                }
                is Resource.Success<*> -> {
                    _updateParentState.value = UpdateParentState.Success(result.data ?: "Parent updated successfully")
                }
            }
        }
    }

    fun deleteParent(parentId: String, email: String) = viewModelScope.launch {
        adminRepository.deleteParent(parentId = parentId, parentEmail = email).collect{ result ->
            when(result){
                is Resource.Error -> {
                    _deleteParentState.value = DeleteParentState.Error(result.message!!)
                }
                is Resource.Loading -> {
                    _deleteParentState.value = DeleteParentState.Loading
                }
                is Resource.Success -> {
                    _deleteParentState.value = DeleteParentState.Success(result.data ?: "Parent deleted successfully")
                }
            }
        }
    }

    fun resetAddParentState() {
        _addParentState.value = AddParentState.Idle
    }
}


sealed class AddParentState {
    object Idle : AddParentState()
    object Loading : AddParentState()
    data class Error(val message: String) : AddParentState()
    data class Success(val message: String) : AddParentState()
}

sealed class GetParentState {
    object Loading : GetParentState()
    data class Error(val message: String) : GetParentState()
    data class Success(val parents: List<Parent>) : GetParentState()
}

sealed class UpdateParentState{
    object Idle : UpdateParentState()
    object Loading : UpdateParentState()
    data class Error(val message: String) : UpdateParentState()
    data class Success(val message: String) : UpdateParentState()
}

sealed class DeleteParentState{
    object Idle : DeleteParentState()
    object Loading : DeleteParentState()
    data class Error(val message: String) : DeleteParentState()
    data class Success(val message: String) : DeleteParentState()
}