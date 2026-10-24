package com.example.rickandmortymvvm.presentation.character.mapper

import com.example.rickandmortymvvm.domain.character.model.Character
import com.example.rickandmortymvvm.domain.character.model.GenderFilter
import com.example.rickandmortymvvm.presentation.character.model.CharacterGender
import com.example.rickandmortymvvm.presentation.character.model.CharacterUiModel

/**
 * Creates display values before publication to the UI and maps user filters into domain inputs.
 *
 * @author Stefan Bayne
 * @since 2026-10-24
 */
fun Character.toUiModel(): CharacterUiModel =
    CharacterUiModel(
        id = id,
        name = name,
        imageUrl = image,
        species = species.toLabel(),
        gender = gender.toLabel(),
        status = status.toLabel(),
        firstSeenDate = created.substringBefore('T'),
    )

fun CharacterGender.toDomain(): GenderFilter =
    when (this) {
        CharacterGender.ALL -> GenderFilter.ALL
        CharacterGender.MALE -> GenderFilter.MALE
        CharacterGender.FEMALE -> GenderFilter.FEMALE
    }

private fun String.toLabel(): String = replaceFirstChar {
    if (it.isLowerCase()) it.titlecase() else it.toString()
}
