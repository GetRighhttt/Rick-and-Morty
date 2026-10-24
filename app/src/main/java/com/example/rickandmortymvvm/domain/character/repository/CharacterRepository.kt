package com.example.rickandmortymvvm.domain.character.repository

import androidx.paging.PagingData
import com.example.rickandmortymvvm.domain.character.model.Character
import com.example.rickandmortymvvm.domain.character.model.GenderFilter
import kotlinx.coroutines.flow.Flow

/**
 * Defines character access for ViewModels; implementations own remote and local data conversion.
 *
 * @author Stefan Bayne
 * @since 2026-10-24
 */
interface CharacterRepository {
    fun searchCharacters(query: String, gender: GenderFilter): Flow<PagingData<Character>>

    fun savedCharacters(): Flow<List<Character>>

    suspend fun getCharacter(id: Int): Character

    suspend fun saveCharacter(character: Character)

    suspend fun deleteCharacter(id: Int)

    suspend fun deleteAllCharacters()
}
