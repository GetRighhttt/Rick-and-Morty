package com.example.rickandmortymvvm.data.api

import com.example.rickandmortymvvm.data.character.model.CharacterDto
import com.example.rickandmortymvvm.data.character.model.CharacterPageDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Defines remote character endpoints using data-layer response DTOs.
 *
 * @author Stefan Bayne
 * @since 2023-05-01
 */
interface RickAndMortyApiService {
    @GET("character/{id}") suspend fun getCharacter(@Path("id") id: Int): Response<CharacterDto>

    @GET("character/")
    suspend fun searchAllCharacters(
        @Query("name") name: String,
        @Query("page") page: Int,
        @Query("gender") gender: String,
    ): Response<CharacterPageDto>
}
