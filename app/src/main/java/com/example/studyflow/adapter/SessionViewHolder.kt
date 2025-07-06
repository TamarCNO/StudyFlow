package com.example.studyflow.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.studyflow.R

class SessionViewHolder(itemView: View, private val listener: SessionsAdapter(
private val sessions: <liveData<List<Session>>,
private val listener: SessionsViewHolder.OnItemClickListener
) : RecyclerView.Adapter<SessionsViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.session_row, parent, false)
        return SessionsViewHolder(view, listener)
    }

    override fun onBindViewHolder(holder: SessionsViewHolder, position: Int) {
        holder.bind(sessions[position], position)
    }

    override fun getItemCount(): Int = sessions.size
}

