package com.example.studyflow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.studyflow.adapters.SessionsAdapter
import com.example.studyflow.models.Model
import com.example.studyflow.models.Session
import com.example.studyflow.models.Student

class SessionsFragmentList : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SessionsAdapter

    private var sessions: List<Session> = listOf()
    private var students: List<Student> = listOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_sessions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.sessionsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        Model.instance.getAllStudents { students ->
            this.students = students

            Model.instance.getAllSessions { sessions ->
                this.sessions = sessions

                adapter = SessionsAdapter(sessions, object : SessionsViewHolder.OnItemClickListener {
                    override fun onItemClick(position: Int) {
                        val session = sessions[position]
                        // כאן אפשר לטפל בלחיצה על session
                    }
                })

                recyclerView.adapter = adapter
            }
        }
    }
}
