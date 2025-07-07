package com.example.studyflow.adapter

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.studyflow.R
import com.example.studyflow.model.Session
import com.example.studyflow.model.Student

class SessionsViewHolder(
    itemView: View,
    private val listener: OnItemClickListener
) : RecyclerView.ViewHolder(itemView) {

    private val topicTextView: TextView = itemView.findViewById(R.id.session_row_Topic)
    private val dateTextView: TextView = itemView.findViewById(R.id.session_row_Date)
    private val statusTextView: TextView = itemView.findViewById(R.id.session_row_Status)

    fun bind(session: Session, student: Student?, position: Int) {
        topicTextView.text = session.topic
        dateTextView.text = session.date.toString()
        statusTextView.text = session.status

        itemView.setOnClickListener {
            listener.onItemClick(position)
        }
    }

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }
}
