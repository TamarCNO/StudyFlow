package com.example.studyflow

class SessionsFragmentList {
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

        // קודם טוען את הסטודנטים
        Model.instance.getAllStudents { students ->
            this.students = students

            // ואז טוען את ה-sessions
            Model.instance.getAllSessions { sessions ->
                this.sessions = sessions

                adapter = SessionsAdapter(sessions, object : SessionsViewHolder.OnItemClickListener {
                    override fun onItemClick(position: Int) {
                        val session = sessions[position]
                        // תוכל לעשות משהו עם ה-session שנבחר
                    }
                })

                recyclerView.adapter = adapter
            }
        }
    }
}
