package com.example.studyflow.model.dao

import androidx.room.*
import com.example.studyflow.model.PostEntity

@Dao
interface PostDao {

    @Query("SELECT * FROM posts")
    suspend fun getAllPosts(): List<PostEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(posts: List<PostEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(post: PostEntity)

    @Query("DELETE FROM posts")
    suspend fun deleteAll()
}
