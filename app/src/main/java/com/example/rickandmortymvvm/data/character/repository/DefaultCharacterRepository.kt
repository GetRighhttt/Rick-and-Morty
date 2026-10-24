package com.example.rickandmortymvvm.data.character.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.map
import com.example.rickandmortymvvm.core.util.Constants
import com.example.rickandmortymvvm.data.api.RMPagingSource
import com.example.rickandmortymvvm.data.api.RickAndMortyApiService
import com.example.rickandmortymvvm.data.character.mapper.toDomain
import com.example.rickandmortymvvm.data.character.mapper.toEntity
import com.example.rickandmortymvvm.data.db.CharacterDAO
import com.example.rickandmortymvvm.domain.character.model.Character
import com.example.rickandmortymvvm.domain.character.model.GenderFilter
import com.example.rickandmortymvvm.domain.character.repository.CharacterRepository
import java.util.Collections
import kotlinx.coroutines.flow.map
import retrofit2.HttpException

/**
 * Maps remote pages and saved records to domain characters; Room and Retrofit own their IO
 * dispatching.
 *
 * @author Stefan Bayne
 * @since 2023-05-01
 */
class DefaultCharacterRepository(
    private val api: RickAndMortyApiService,
    private val dao: CharacterDAO,
) : CharacterRepository {
    private val characterCache =
        Collections.synchronizedMap(
            object : LinkedHashMap<Int, Character>(100, 0.75f, true) {
                override fun removeEldestEntry(
                    eldest: MutableMap.MutableEntry<Int, Character>?
                ): Boolean = size > 100
            }
        )

    override fun searchCharacters(query: String, gender: GenderFilter) =
        Pager(
                config =
                    PagingConfig(
                        pageSize = Constants.PAGE_SIZE,
                        initialLoadSize = Constants.PAGE_SIZE,
                        maxSize = 100,
                        enablePlaceholders = false,
                    ),
                pagingSourceFactory = { RMPagingSource(api, query, gender.toApiValue()) },
            )
            .flow
            .map { page ->
                page.map { dto ->
                    dto.toDomain().also { characterCache[it.id] = it }
                }
            }

    override fun savedCharacters() =
        dao.getAllCharacters().map { records ->
            records.map { it.toDomain() }
        }

    override suspend fun getCharacter(id: Int): Character {
        dao.getCharacter(id)?.let {
            return it.toDomain()
        }
        characterCache[id]?.let {
            return it
        }
        val response = api.getCharacter(id)
        if (!response.isSuccessful) throw HttpException(response)
        return checkNotNull(response.body()) { "Character response body was empty" }
            .toDomain()
            .also { characterCache[it.id] = it }
    }

    override suspend fun saveCharacter(character: Character) = dao.insert(character.toEntity())

    override suspend fun deleteCharacter(id: Int) = dao.deleteCharacter(id)

    override suspend fun deleteAllCharacters() = dao.deleteAll()

    private fun GenderFilter.toApiValue(): String =
        when (this) {
            GenderFilter.ALL -> ""
            GenderFilter.MALE -> "male"
            GenderFilter.FEMALE -> "female"
        }
}
