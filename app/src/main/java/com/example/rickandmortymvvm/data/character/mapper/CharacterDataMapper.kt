package com.example.rickandmortymvvm.data.character.mapper

import com.example.rickandmortymvvm.data.character.model.CharacterDto
import com.example.rickandmortymvvm.data.character.model.CharacterEntity
import com.example.rickandmortymvvm.domain.character.model.Character

/**
 * Converts data records at the repository boundary without sharing storage or transport models.
 *
 * @author Stefan Bayne
 * @since 2026-10-24
 */
fun CharacterDto.toDomain(): Character =
    Character(
        id = id,
        created = created,
        gender = gender,
        image = image,
        name = name,
        species = species,
        status = status,
        type = type,
        url = url,
    )

fun CharacterEntity.toDomain(): Character =
    Character(
        id = id,
        created = created,
        gender = gender,
        image = image,
        name = name,
        species = species,
        status = status,
        type = type,
        url = url,
    )

fun Character.toEntity(): CharacterEntity =
    CharacterEntity(
        id = id,
        created = created,
        gender = gender,
        image = image,
        name = name,
        species = species,
        status = status,
        type = type,
        url = url,
    )
