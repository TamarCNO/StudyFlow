package com.example.studyflow.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.studyflow.R
import com.example.studyflow.model.Session

class SessionsAdapter(
    private var sessions: List<Session>,
    private val listener: SessionsViewHolder.OnItemClickListener
) : RecyclerView.Adapter<SessionsViewHolder>() {

    fun set(newSessions: List<Session>) {
        sessions = newSessions
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_session_row, parent, false)
        return SessionsViewHolder(view, listener)
    }

    override fun onBindViewHolder(holder: SessionsViewHolder, position: Int) {
        val session = sessions[position]
        holder.bind(session)
    }

    override fun getItemCount(): Int = sessions.size
}
