package com.studypulse.sync.ui.packages

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studypulse.sync.StudyPulseApplication
import com.studypulse.sync.data.local.entity.StudyPackageEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PackagesUiState(
    val packages: List<StudyPackageEntity> = emptyList(),
    val isRefreshing: Boolean = false,
    val downloadingPackageId: String? = null,
    val errorMessage: String? = null
)

class PackagesViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val container = (application as StudyPulseApplication).container
    private val repository = container.packageRepository

    private val _uiState = MutableStateFlow(PackagesUiState())
    val uiState: StateFlow<PackagesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.packages.collect { packages ->
                _uiState.update { currentState ->
                    currentState.copy(packages = packages)
                }
            }
        }

        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            if (_uiState.value.isRefreshing) return@launch

            _uiState.update { currentState ->
                currentState.copy(
                    isRefreshing = true,
                    errorMessage = null
                )
            }

            val result = repository.refresh()

            _uiState.update { currentState ->
                currentState.copy(
                    isRefreshing = false,
                    errorMessage = result.exceptionOrNull()?.message ?: run {
                        if (result.isFailure) "Unknown refresh error" else null
                    }
                )
            }
        }
    }

    fun downloadPackage(externalId: String) {
        viewModelScope.launch {
            if (_uiState.value.downloadingPackageId != null) return@launch

            _uiState.update { currentState ->
                currentState.copy(
                    downloadingPackageId = externalId,
                    errorMessage = null
                )
            }

            val result = repository.downloadPackage(externalId)

            _uiState.update { currentState ->
                currentState.copy(
                    downloadingPackageId = null,
                    errorMessage = result.exceptionOrNull()?.message ?: run {
                        if (result.isFailure) "Unknown download error" else null
                    }
                )
            }
        }
    }
}