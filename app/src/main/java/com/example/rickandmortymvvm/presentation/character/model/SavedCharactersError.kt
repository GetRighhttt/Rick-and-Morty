package com.example.rickandmortymvvm.presentation.character.model

/**
 * Identifies which saved collection action failed so the screen can explain the failure.
 *
 * @author Stefan Bayne
 * @since 2026-10-24
 */
enum class SavedCharactersError {
    LOAD,
    SAVE,
    DELETE,
    CLEAR,
}
