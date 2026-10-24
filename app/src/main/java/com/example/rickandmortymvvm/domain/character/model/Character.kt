package com.example.rickandmortymvvm.domain.character.model

/**
 * Represents character information independently of JSON, Room, Android views, and navigation.
 *
 * @author Stefan Bayne
 * @since 2026-10-24
 */
data class Character(
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
