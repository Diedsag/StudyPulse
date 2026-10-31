package com.studypulse.sync.ui.materials

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studypulse.sync.StudyPulseApplication
import com.studypulse.sync.data.local.entity.MaterialEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MaterialsUiState(
    val materials: List<MaterialEntity> = emptyList()
)

class MaterialsViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val container = (application as StudyPulseApplication).container
    private val repository = container.studyRepository

    private val _uiState = MutableStateFlow(MaterialsUiState())
    val uiState: StateFlow<MaterialsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.materials.collect { materials ->
                _uiState.update { currentState ->
                    currentState.copy(materials = materials)
                }
            }
        }
    }
}