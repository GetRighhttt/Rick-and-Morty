package com.example.rickandmortymvvm.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rickandmortymvvm.domain.character.model.Character
import com.example.rickandmortymvvm.domain.character.repository.CharacterRepository
import com.example.rickandmortymvvm.presentation.character.mapper.toUiModel
import com.example.rickandmortymvvm.presentation.character.model.RemovedCharacterUiModel
import com.example.rickandmortymvvm.presentation.character.model.SavedCharactersError
import com.example.rickandmortymvvm.presentation.character.model.SavedCharactersUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Publishes collection state, serializes writes, and retains the deleted domain character for undo.
 *
 * @author Stefan Bayne
 * @since 2023-05-22
 */
@HiltViewModel
class SavedViewModel @Inject constructor(private val repository: CharacterRepository) :
    ViewModel() {
    private val _uiState = MutableStateFlow(SavedCharactersUiState())
    val uiState = _uiState.asStateFlow()
    private var savedCharacters: List<Character> = emptyList()
    private var lastDeletedCharacter: Character? = null
    private var observationJob: Job? = null
    private var operationJob: Job? = null

    init {
        observeCharacters()
    }

    fun observeCharacters() {
        observationJob?.cancel()
        observationJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                repository.savedCharacters().collect { characters ->
                    savedCharacters = characters
                    _uiState.update {
                        it.copy(
                            characters = characters.map { character -> character.toUiModel() },
                            isLoading = false,
                            error =
                                it.error.takeUnless { error -> error == SavedCharactersError.LOAD },
                        )
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false, error = SavedCharactersError.LOAD) }
            }
        }
    }

    fun deleteCharacter(id: Int) {
        val character = savedCharacters.firstOrNull { it.id == id } ?: return
        updateCollection(SavedCharactersError.DELETE) {
            repository.deleteCharacter(id)
            lastDeletedCharacter = character
            _uiState.update {
                it.copy(removal = RemovedCharacterUiModel(character.id, character.name))
            }
        }
    }

    fun undoDelete(id: Int) {
        viewModelScope.launch {
            operationJob?.join()
            val character = lastDeletedCharacter?.takeIf { it.id == id } ?: return@launch
            updateCollection(SavedCharactersError.SAVE) {
                repository.saveCharacter(character)
                lastDeletedCharacter = null
            }
        }
    }

    fun deleteAllCharacters() =
        updateCollection(SavedCharactersError.CLEAR) {
            repository.deleteAllCharacters()
            lastDeletedCharacter = null
        }

    fun acknowledgeRemoval() {
        _uiState.update { it.copy(removal = null) }
    }

    fun acknowledgeError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun updateCollection(error: SavedCharactersError, action: suspend () -> Unit) {
        if (operationJob?.isActive == true) return
        _uiState.update { it.copy(isUpdating = true, error = null) }
        operationJob = viewModelScope.launch {
            try {
                action()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(error = error) }
            } finally {
                _uiState.update { it.copy(isUpdating = false) }
            }
        }
    }
}
