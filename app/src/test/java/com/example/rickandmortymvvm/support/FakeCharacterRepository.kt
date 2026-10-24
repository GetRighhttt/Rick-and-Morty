package com.example.rickandmortymvvm.support

import androidx.paging.PagingData
import com.example.rickandmortymvvm.domain.character.model.Character
import com.example.rickandmortymvvm.domain.character.model.GenderFilter
import com.example.rickandmortymvvm.domain.character.repository.CharacterRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

/**
 * Provides controlled reads, writes, and failures without Android storage or network dependencies.
 *
 * @author Stefan Bayne
 * @since 2026-10-24
 */
class FakeCharacterRepository : CharacterRepository {
    val saved = MutableStateFlow<List<Character>>(emptyList())
    var loadedCharacter = characterFixture()
    var loadFailure: Exception? = null
    var writeFailure: Exception? = null
    var saveGate: CompletableDeferred<Unit>? = null
    var loadGate: CompletableDeferred<Unit>? = null
    var saveCalls = 0
    var savedValue: Character? = null

    override fun searchCharacters(query: String, gender: GenderFilter) =
        flowOf(PagingData.from(listOf(loadedCharacter)))

    override fun savedCharacters() = saved

    override suspend fun getCharacter(id: Int): Character {
        loadGate?.await()
        loadFailure?.let { throw it }
        return loadedCharacter.copy(id = id)
    }

    override suspend fun saveCharacter(character: Character) {
        saveCalls++
        saveGate?.await()
        writeFailure?.let { throw it }
        savedValue = character
        saved.value = saved.value.filterNot { it.id == character.id } + character
    }

    override suspend fun deleteCharacter(id: Int) {
        writeFailure?.let { throw it }
        saved.value = saved.value.filterNot { it.id == id }
    }

    override suspend fun deleteAllCharacters() {
        writeFailure?.let { throw it }
        saved.value = emptyList()
    }
}
