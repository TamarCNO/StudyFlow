package com.example.studyflow.model.repository

import com.example.studyflow.model.Session
import com.example.studyflow.model.dao.SessionDao

class SessionRepository(private val sessionDao: SessionDao) {

    fun getAllSessions() = sessionDao.getAll()

    fun getSessionById(id: String) = sessionDao.getSessionById(id)

    suspend fun insertSession(session: Session) {
        sessionDao.insertAll(session)
    }

    suspend fun deleteSession(session: Session) {
        sessionDao.delete(session)
    }
}
