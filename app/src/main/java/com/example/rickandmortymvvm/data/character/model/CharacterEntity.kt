package com.example.rickandmortymvvm.data.character.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores a saved character using the existing version 1 table and column definitions.
 *
 * @author Stefan Bayne
 * @since 2026-10-24
 */
@Entity(tableName = "characters")
data class CharacterEntity(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val created: String,
    val gender: String,
    val image: String,
    val name: String,
    val species: String,
    val status: String,
    val type: String,
    val url: String,
)
