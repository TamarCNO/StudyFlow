package com.example.studyflow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class DetailsFragment : Fragment() {

    private lateinit var topicTextView: TextView
    private lateinit var dateTextView: TextView
    private lateinit var timeTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_session_details, container, false)

        topicTextView = view.findViewById(R.id.session_topic_text_view)
        dateTextView = view.findViewById(R.id.session_date_text_view)
        timeTextView = view.findViewById(R.id.session_time_text_view)


        return view
    }
}
