package com.example.rickandmortymvvm.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.rickandmortymvvm.data.character.model.CharacterEntity
import kotlinx.coroutines.flow.Flow

/**
 * Exposes suspend writes and observable entity records; no domain or UI models enter Room.
 *
 * @author Stefan Bayne
 * @since 2023-05-22
 */
@Dao
interface CharacterDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(character: CharacterEntity)

    @Query("SELECT * FROM characters ORDER BY id")
    fun getAllCharacters(): Flow<List<CharacterEntity>>

    @Query("SELECT * FROM characters WHERE id = :id")
    suspend fun getCharacter(id: Int): CharacterEntity?

    @Query("DELETE FROM characters") suspend fun deleteAll()

    @Query("DELETE FROM characters WHERE id = :id") suspend fun deleteCharacter(id: Int)
}
