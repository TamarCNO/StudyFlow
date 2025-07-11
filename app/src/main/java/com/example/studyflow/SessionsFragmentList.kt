package com.example.studyflow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.studyflow.adapter.SessionsAdapter
import com.example.studyflow.adapter.SessionsViewHolder
import com.example.studyflow.model.Model
import com.example.studyflow.model.Session
import com.example.studyflow.model.Student

class SessionsFragmentList : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SessionsAdapter

    private var sessions: List<Session> = listOf()
    private var students: List<Student> = listOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_sessions_recycler_view, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.sessionsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        Model.shared.getAllStudents { students ->
            this.students = students

            Model.shared.getAllSessions { sessions ->
                this.sessions = sessions

                adapter = SessionsAdapter(sessions,students, object : SessionsViewHolder.OnItemClickListener {
                    override fun onItemClick(position: Int) {
                        val session = sessions[position]
                    }
                })

                recyclerView.adapter = adapter
            }
        }
    }
}
