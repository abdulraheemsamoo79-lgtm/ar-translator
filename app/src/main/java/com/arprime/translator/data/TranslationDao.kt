package com.arprime.translator.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TranslationDao {

    @Query("SELECT * FROM translations ORDER BY timestamp DESC")
    fun getAll(): Flow<List<TranslationEntity>>

    @Query("SELECT * FROM translations WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavorites(): Flow<List<TranslationEntity>>

    @Query("SELECT * FROM translations WHERE sourceText LIKE '%' || :query || '%' OR translatedText LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun search(query: String): Flow<List<TranslationEntity>>

    @Insert
    suspend fun insert(item: TranslationEntity): Long

    @Update
    suspend fun update(item: TranslationEntity)

    @Delete
    suspend fun delete(item: TranslationEntity)

    @Query("DELETE FROM translations")
    suspend fun clearAll()

    @Query("DELETE FROM translations WHERE isFavorite = 0")
    suspend fun clearNonFavorites()
}
