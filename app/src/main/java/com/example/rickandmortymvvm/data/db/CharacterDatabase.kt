package com.example.rickandmortymvvm.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.rickandmortymvvm.data.character.model.CharacterEntity

/**
 * Owns the existing saved-character schema; entity separation does not change the table.
 *
 * @author Stefan Bayne
 * @since 2023-05-22
 */
@Database(entities = [CharacterEntity::class], version = 1, exportSchema = false)
abstract class CharacterDatabase : RoomDatabase() {
    abstract fun getCharacterDAO(): CharacterDAO
}
