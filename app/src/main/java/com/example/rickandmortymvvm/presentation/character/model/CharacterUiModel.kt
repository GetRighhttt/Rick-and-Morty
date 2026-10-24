package com.example.rickandmortymvvm.presentation.character.model

/**
 * Contains only the immutable values needed to display a character; navigation and actions use its
 * ID.
 *
 * @author Stefan Bayne
 * @since 2026-10-24
 */
data class CharacterUiModel(
    val id: Int,
    val name: String,
    val imageUrl: String,
    val species: String,
    val gender: String,
    val status: String,
    val firstSeenDate: String,
)
