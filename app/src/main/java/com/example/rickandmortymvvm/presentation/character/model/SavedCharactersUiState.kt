package com.example.rickandmortymvvm.presentation.character.model

/**
 * Keeps collection content, progress, errors, and an unacknowledged removal message in one screen
 * state.
 *
 * @author Stefan Bayne
 * @since 2026-10-24
 */
data class SavedCharactersUiState(
    val characters: List<CharacterUiModel> = emptyList(),
    val isLoading: Boolean = true,
    val isUpdating: Boolean = false,
    val error: SavedCharactersError? = null,
    val removal: RemovedCharacterUiModel? = null,
)
