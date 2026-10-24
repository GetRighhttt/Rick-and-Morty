package com.example.rickandmortymvvm.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rickandmortymvvm.domain.character.model.Character
import com.example.rickandmortymvvm.domain.character.repository.CharacterRepository
import com.example.rickandmortymvvm.presentation.character.mapper.toUiModel
import com.example.rickandmortymvvm.presentation.character.model.CharacterSaveResult
import com.example.rickandmortymvvm.presentation.character.model.DetailsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Loads a profile by its restored ID and reports save success only after persistence completes.
 *
 * @author Stefan Bayne
 * @since 2023-05-05
 */
@HiltViewModel
class DetailsViewModel
@Inject
constructor(
    private val repository: CharacterRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DetailsUiState())
    val uiState = _uiState.asStateFlow()
    private var character: Character? = null
    private var loadJob: Job? = null

    init {
        loadCharacter()
    }

    fun loadCharacter() {
        if (_uiState.value.isSaving) return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            character = null
            _uiState.value = DetailsUiState()
            try {
                val id = requireNotNull(savedStateHandle.get<Int>(CHARACTER_ID))
                val loaded = repository.getCharacter(id)
                character = loaded
                _uiState.value = DetailsUiState(character = loaded.toUiModel(), isLoading = false)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.value = DetailsUiState(isLoading = false, loadFailed = true)
            }
        }
    }

    fun saveCharacter() {
        val currentCharacter = character ?: return
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true, saveResult = null) }
        viewModelScope.launch {
            try {
                repository.saveCharacter(currentCharacter)
                _uiState.update {
                    it.copy(isSaving = false, saveResult = CharacterSaveResult.SAVED)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, saveResult = CharacterSaveResult.FAILED)
                }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun acknowledgeSaveResult() {
        _uiState.update { it.copy(saveResult = null) }
    }

    companion object {
        const val CHARACTER_ID = "character_id"
    }
}
