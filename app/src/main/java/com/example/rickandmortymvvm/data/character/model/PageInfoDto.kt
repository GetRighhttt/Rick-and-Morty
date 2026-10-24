package com.example.rickandmortymvvm.data.character.model

/**
 * Holds the API pagination links used to decide whether more pages exist.
 *
 * @author Stefan Bayne
 * @since 2026-10-24
 */
data class PageInfoDto(
    val count: Int,
    val pages: Int,
    val next: String?,
    val prev: String?,
)
