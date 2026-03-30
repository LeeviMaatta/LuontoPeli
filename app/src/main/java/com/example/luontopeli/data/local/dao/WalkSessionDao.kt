package com.example.luontopeli.data.local.dao

import androidx.room.*
import com.example.luontopeli.data.local.entity.WalkSession
import kotlinx.coroutines.flow.Flow

@Dao
interface WalkSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: WalkSession)

    @Update
    suspend fun update(session: WalkSession)

    @Query("SELECT * FROM walk_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<WalkSession>>

    @Query("SELECT * FROM walk_sessions WHERE isActive = 1 LIMIT 1")
    fun getActiveSession(): Flow<WalkSession?>

    @Query("DELETE FROM walk_sessions WHERE id = :id")
    suspend fun deleteById(id: String)
}