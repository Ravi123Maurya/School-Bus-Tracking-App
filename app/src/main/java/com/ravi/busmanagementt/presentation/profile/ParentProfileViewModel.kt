package com.ravi.busmanagementt.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ravi.busmanagementt.domain.model.Parent
import com.ravi.busmanagementt.domain.repository.UserRepository
import com.ravi.busmanagementt.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class ParentProfileViewModel @Inject constructor(
    private val userRepository: UserRepository
): ViewModel() {

    private val _parentProfileState = MutableStateFlow<GetParentProfileState>(GetParentProfileState.Loading)
    val parentProfileState = _parentProfileState.asStateFlow()

    init {
        getParentProfile()
    }

    fun getParentProfile() = viewModelScope.launch{
        userRepository.getParentProfile().collect { result ->
            when(result){
                is Resource.Error -> {
                    _parentProfileState.value = GetParentProfileState.Error(result.message.toString())
                }
                is Resource.Loading -> {
                    _parentProfileState.value = GetParentProfileState.Loading
                }
                is Resource.Success<*> -> {
                    _parentProfileState.value = GetParentProfileState.Success(result.data as Parent)
                }
            }
        }
    }



}

sealed class GetParentProfileState{
    object Loading: GetParentProfileState()
    data class Success(val parent: Parent): GetParentProfileState()
    data class Error(val message: String): GetParentProfileState()
}