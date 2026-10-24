package com.example.rickandmortymvvm.data.character.repository

import com.example.rickandmortymvvm.data.api.RickAndMortyApiService
import com.example.rickandmortymvvm.data.character.model.CharacterDto
import com.example.rickandmortymvvm.data.character.model.CharacterEntity
import com.example.rickandmortymvvm.data.character.model.CharacterPageDto
import com.example.rickandmortymvvm.data.db.CharacterDAO
import com.example.rickandmortymvvm.support.characterFixture
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/**
 * Verifies offline lookup, HTTP failures, identity preservation, and field mappings across
 * repository boundaries.
 *
 * @author Stefan Bayne
 * @since 2026-10-24
 */
class DefaultCharacterRepositoryTest {
    private val dto =
        CharacterDto(
            id = 42,
            created = "2017-11-04T18:48:46.250Z",
            gender = "unknown",
            image = "https://example.com/42.png",
            name = "Test character 42",
            species = "alien",
            status = "unknown",
            type = "Original type",
            url = "https://example.com/character/42",
        )
    private val entity =
        CharacterEntity(
            id = 42,
            created = "2017-11-04T18:48:46.250Z",
            gender = "unknown",
            image = "https://example.com/42.png",
            name = "Test character 42",
            species = "alien",
            status = "unknown",
            type = "Original type",
            url = "https://example.com/character/42",
        )

    @Test
    fun `saved profile maps the local entity without requesting the network`() = runTest {
        val api = FakeApi(Response.success(dto))
        val dao =
            FakeDao().apply { records.value = listOf(entity.copy(name = "Offline character")) }
        val result = DefaultCharacterRepository(api, dao).getCharacter(42)
        assertEquals(0, api.detailRequests)
        assertEquals("Offline character", result.name)
        assertEquals(42, result.id)
        assertEquals(entity.created, result.created)
        assertEquals(entity.gender, result.gender)
        assertEquals(entity.image, result.image)
        assertEquals(entity.species, result.species)
        assertEquals(entity.status, result.status)
        assertEquals(entity.type, result.type)
        assertEquals(entity.url, result.url)
    }

    @Test
    fun `remote profile maps every field to a domain character`() = runTest {
        val api = FakeApi(Response.success(dto))
        val result = DefaultCharacterRepository(api, FakeDao()).getCharacter(42)
        assertEquals(1, api.detailRequests)
        assertEquals(characterFixture(), result)
    }

    @Test
    fun `a loaded profile can reopen from the process cache`() = runTest {
        val api = FakeApi(Response.success(dto))
        val repository = DefaultCharacterRepository(api, FakeDao())
        repository.getCharacter(42)
        val reopened = repository.getCharacter(42)
        assertEquals(1, api.detailRequests)
        assertEquals(characterFixture(), reopened)
    }

    @Test
    fun `save uses the API identity and original values in the entity`() = runTest {
        val dao = FakeDao()
        val repository = DefaultCharacterRepository(FakeApi(Response.success(dto)), dao)
        repository.saveCharacter(characterFixture())
        assertEquals(entity, dao.records.value.single())
        repository.saveCharacter(characterFixture().copy(name = "Updated"))
        assertEquals(1, dao.records.value.size)
        assertEquals(42, dao.records.value.single().id)
        assertEquals("unknown", dao.records.value.single().status)
    }

    @Test
    fun `saved stream exposes domain fields rather than storage records`() = runTest {
        val dao = FakeDao().apply { records.value = listOf(entity) }
        val result =
            DefaultCharacterRepository(FakeApi(Response.success(dto)), dao)
                .savedCharacters()
                .first()
        assertEquals(listOf(characterFixture()), result)
    }

    @Test
    fun `remote profile HTTP errors are propagated for UI retry`() = runTest {
        val response =
            Response.error<CharacterDto>(
                503,
                "unavailable".toResponseBody("text/plain".toMediaType()),
            )
        try {
            DefaultCharacterRepository(FakeApi(response), FakeDao()).getCharacter(42)
            fail("Expected a network failure")
        } catch (error: HttpException) {
            assertEquals(503, error.code())
        }
    }

    private class FakeApi(private val response: Response<CharacterDto>) : RickAndMortyApiService {
        var detailRequests = 0

        override suspend fun getCharacter(id: Int): Response<CharacterDto> {
            detailRequests++
            return response
        }

        override suspend fun searchAllCharacters(
            name: String,
            page: Int,
            gender: String,
        ): Response<CharacterPageDto> = error("These tests only use the profile endpoint")
    }

    private class FakeDao : CharacterDAO {
        val records = MutableStateFlow<List<CharacterEntity>>(emptyList())

        override fun getAllCharacters() = records

        override suspend fun getCharacter(id: Int) = records.value.firstOrNull { it.id == id }

        override suspend fun insert(character: CharacterEntity) {
            records.value = records.value.filterNot { it.id == character.id } + character
        }

        override suspend fun deleteAll() {
            records.value = emptyList()
        }

        override suspend fun deleteCharacter(id: Int) {
            records.value = records.value.filterNot { it.id == id }
        }
    }
}
