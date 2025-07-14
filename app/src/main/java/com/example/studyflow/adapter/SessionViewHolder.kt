package com.example.studyflow.adapter

import androidx.recyclerview.widget.RecyclerView
import com.example.studyflow.databinding.FragmentSessionRowBinding
import com.example.studyflow.model.Session

class SessionsViewHolder(
    private val binding: FragmentSessionRowBinding,
    private val listener: OnItemClickListener?
) : RecyclerView.ViewHolder(binding.root) {

    private var session: Session? = null

    init {
        itemView.setOnClickListener {
            session?.let {
                listener?.onItemClick(it)
            }
        }
    }

    fun bind(session: Session?) {
        this.session = session
        session?.let {
            binding.sessionRowTopic.text = it.topic
            binding.sessionRowDate.text = "Date: ${it.date}"
            binding.sessionRowStatus.text = "Status: ${it.status}"
            binding.sessionRowStudentEmail.text = "Student: ${
                if (it.studentEmail.isNullOrEmpty()) "N/A" else it.studentEmail
            }"
        }
    }

    interface OnItemClickListener {
        fun onItemClick(session: Session)
    }
}
