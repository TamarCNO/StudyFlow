package com.example.studyflow.model.repository

import com.example.studyflow.model.PostEntity
import com.example.studyflow.model.dao.PostDao

class PostRepository(private val postDao: PostDao) {

    suspend fun getAllPosts(): List<PostEntity> = postDao.getAllPosts()

    suspend fun insertAllPosts(posts: List<PostEntity>) = postDao.insertAll(posts)

    suspend fun insertPost(post: PostEntity) = postDao.insert(post)

    suspend fun deleteAllPosts() = postDao.deleteAll()
}
