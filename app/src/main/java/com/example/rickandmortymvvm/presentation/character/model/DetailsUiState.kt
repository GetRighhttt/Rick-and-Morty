package com.example.rickandmortymvvm.presentation.character.model

/**
 * Publishes the profile and its loading and save state as one consistent screen value.
 *
 * @author Stefan Bayne
 * @since 2026-10-24
 */
data class DetailsUiState(
    val character: CharacterUiModel? = null,
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val isSaving: Boolean = false,
    val saveResult: CharacterSaveResult? = null,
)
