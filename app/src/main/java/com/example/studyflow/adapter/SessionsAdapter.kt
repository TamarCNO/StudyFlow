package com.example.studyflow.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.studyflow.databinding.FragmentSessionRowBinding
import com.example.studyflow.model.Session

class SessionsAdapter(private var sessions: List<Session>?) : RecyclerView.Adapter<SessionsViewHolder>() {

    var listener: SessionsViewHolder.OnItemClickListener? = null

    fun set(sessions: List<Session>?) {
        this.sessions = sessions
    }

    override fun getItemCount(): Int = sessions?.size ?: 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionsViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = FragmentSessionRowBinding.inflate(inflater, parent, false)
        return SessionsViewHolder(binding, listener)
    }

    override fun onBindViewHolder(holder: SessionsViewHolder, position: Int) {
        holder.bind(sessions?.get(position))
    }
}
