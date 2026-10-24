package com.example.rickandmortymvvm.data.character.model

/**
 * Describes a character returned by the remote API.
 *
 * @author Stefan Bayne
 * @since 2026-10-24
 */
data class CharacterDto(
    val id: Int,
    val created: String,
    val gender: String,
    val image: String,
    val name: String,
    val species: String,
    val status: String,
    val type: String,
    val url: String,
)
