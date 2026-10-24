package com.example.rickandmortymvvm.support

import com.example.rickandmortymvvm.domain.character.model.Character

/**
 * Supplies domain characters with distinct fields for state and undo boundary checks.
 *
 * @author Stefan Bayne
 * @since 2026-10-24
 */
fun characterFixture(id: Int = 42) =
    Character(
        id = id,
        created = "2017-11-04T18:48:46.250Z",
        gender = "unknown",
        image = "https://example.com/$id.png",
        name = "Test character $id",
        species = "alien",
        status = "unknown",
        type = "Original type",
        url = "https://example.com/character/$id",
    )
