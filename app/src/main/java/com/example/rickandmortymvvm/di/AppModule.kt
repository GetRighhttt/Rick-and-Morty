package com.example.rickandmortymvvm.di

import android.app.Application
import androidx.room.Room
import com.example.rickandmortymvvm.core.util.DispatcherProvider
import com.example.rickandmortymvvm.data.api.RetrofitInstance
import com.example.rickandmortymvvm.data.api.RickAndMortyApiService
import com.example.rickandmortymvvm.data.character.repository.DefaultCharacterRepository
import com.example.rickandmortymvvm.data.db.CharacterDAO
import com.example.rickandmortymvvm.data.db.CharacterDatabase
import com.example.rickandmortymvvm.domain.character.repository.CharacterRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Builds API and Room dependencies and supplies the domain repository contract.
 *
 * @author Stefan Bayne
 * @since 2023-05-01
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideApiService(): RickAndMortyApiService = RetrofitInstance.rickAndMortyService

    @Singleton
    @Provides
    fun provideRepository(
        rickAndMortyApiService: RickAndMortyApiService,
        characterDAO: CharacterDAO,
    ): CharacterRepository = DefaultCharacterRepository(rickAndMortyApiService, characterDAO)

    @Singleton
    @Provides
    fun providesDispatcherProvider(): DispatcherProvider =
        object : DispatcherProvider {
            override val mainCD: CoroutineDispatcher
                get() = Dispatchers.Main

            override val ioCD: CoroutineDispatcher
                get() = Dispatchers.IO

            override val defaultCD: CoroutineDispatcher
                get() = Dispatchers.Default

            override val unconfinedCD: CoroutineDispatcher
                get() = Dispatchers.Unconfined
        }

    @Singleton
    @Provides
    fun provideCharacterDatabase(app: Application): CharacterDatabase =
        Room.databaseBuilder(
                app,
                CharacterDatabase::class.java,
                "rickAndMorty_db",
            )
            .build()

    @Singleton
    @Provides
    fun provideDAO(database: CharacterDatabase): CharacterDAO = database.getCharacterDAO()
}
