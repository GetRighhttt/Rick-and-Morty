package com.example.rickandmortymvvm.presentation.character.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Keeps browser inputs together for restoration through SavedStateHandle.
 *
 * @author Stefan Bayne
 * @since 2026-10-24
 */
@Parcelize
data class CharacterFilters(
    val query: String = "",
    val gender: CharacterGender = CharacterGender.ALL,
) : Parcelable
