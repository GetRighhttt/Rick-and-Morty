package com.example.rickandmortymvvm.data.character.model

/**
 * Describes the remote page envelope; pagination metadata stays in the data layer.
 *
 * @author Stefan Bayne
 * @since 2026-10-24
 */
data class CharacterPageDto(
    val info: PageInfoDto,
    val results: List<CharacterDto>,
)
