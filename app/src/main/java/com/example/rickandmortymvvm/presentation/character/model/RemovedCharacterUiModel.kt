package com.example.rickandmortymvvm.presentation.character.model

/**
 * Identifies the removal shown by a snackbar so an old undo action cannot restore a newer deletion.
 *
 * @author Stefan Bayne
 * @since 2026-10-24
 */
data class RemovedCharacterUiModel(val id: Int, val name: String)
