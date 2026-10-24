package com.example.rickandmortymvvm.data.api

import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.rickandmortymvvm.data.character.model.CharacterDto
import com.example.rickandmortymvvm.data.character.model.CharacterPageDto
import com.example.rickandmortymvvm.data.character.model.PageInfoDto
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/**
 * Checks pagination metadata, terminal pages, error classification, and refresh anchors using API
 * DTOs.
 *
 * @author Stefan Bayne
 * @since 2026-08-16
 */
class RMPagingSourceTest {

    @Test
    fun `load uses API metadata for previous and next page keys`() = runBlocking {
        val source =
            pagingSource(
                Response.success(
                    response(
                        results = listOf(character(21)),
                        previous = "https://rickandmortyapi.com/api/character?page=1",
                        next = "https://rickandmortyapi.com/api/character?page=3",
                    )
                )
            )

        val result = source.load(refreshParams(key = 2))

        assertTrue(result is PagingSource.LoadResult.Page)
        result as PagingSource.LoadResult.Page
        assertEquals(1, result.prevKey)
        assertEquals(3, result.nextKey)
    }

    @Test
    fun `load returns no next key on the final page`() = runBlocking {
        val source =
            pagingSource(
                Response.success(
                    response(
                        results = listOf(character(41)),
                        previous = "https://rickandmortyapi.com/api/character?page=1",
                        next = null,
                    )
                )
            )

        val result = source.load(refreshParams(key = 2))

        assertTrue(result is PagingSource.LoadResult.Page)
        result as PagingSource.LoadResult.Page
        assertEquals(1, result.prevKey)
        assertNull(result.nextKey)
    }

    @Test
    fun `404 filter response becomes an empty terminal page`() = runBlocking {
        val source = pagingSource(errorResponse(404))

        val result = source.load(refreshParams())

        assertTrue(result is PagingSource.LoadResult.Page)
        result as PagingSource.LoadResult.Page
        assertTrue(result.data.isEmpty())
        assertNull(result.prevKey)
        assertNull(result.nextKey)
    }

    @Test
    fun `non-404 HTTP response becomes a paging error`() = runBlocking {
        val source = pagingSource(errorResponse(500))

        val result = source.load(refreshParams())

        assertTrue(result is PagingSource.LoadResult.Error)
        result as PagingSource.LoadResult.Error
        assertTrue(result.throwable is HttpException)
        assertEquals(500, (result.throwable as HttpException).code())
    }

    @Test
    fun `refresh key restores the page nearest the visible item`() {
        val source = pagingSource(Response.success(response()))
        val state =
            PagingState(
                pages =
                    listOf(
                        PagingSource.LoadResult.Page(
                            data = listOf(character(21)),
                            prevKey = 1,
                            nextKey = 3,
                        )
                    ),
                anchorPosition = 0,
                config = PagingConfig(pageSize = 20),
                leadingPlaceholderCount = 0,
            )

        assertEquals(2, source.getRefreshKey(state))
    }

    private fun pagingSource(response: Response<CharacterPageDto>) =
        RMPagingSource(
            apiService = FakeApiService(response),
            query = "rick",
            gender = "male",
        )

    private fun refreshParams(key: Int? = null) =
        PagingSource.LoadParams.Refresh(
            key = key,
            loadSize = 20,
            placeholdersEnabled = false,
        )

    private fun response(
        results: List<CharacterDto> = listOf(character(1)),
        previous: String? = null,
        next: String? = null,
    ) =
        CharacterPageDto(
            info =
                PageInfoDto(
                    count = results.size,
                    pages = 1,
                    next = next,
                    prev = previous,
                ),
            results = results,
        )

    private fun character(id: Int) =
        CharacterDto(
            id = id,
            created = "2017-11-04T18:48:46.250Z",
            gender = "Male",
            image = "https://example.com/$id.jpeg",
            name = "Character $id",
            species = "Human",
            status = "Alive",
            type = "",
            url = "https://example.com/character/$id",
        )

    private fun errorResponse(code: Int): Response<CharacterPageDto> =
        Response.error(
            code,
            "{\"error\":\"request failed\"}".toResponseBody("application/json".toMediaType()),
        )

    private class FakeApiService(private val response: Response<CharacterPageDto>) :
        RickAndMortyApiService {
        override suspend fun getCharacter(id: Int): Response<CharacterDto> =
            error("This fake only handles paged requests")

        override suspend fun searchAllCharacters(
            name: String,
            page: Int,
            gender: String,
        ): Response<CharacterPageDto> = response
    }
}
