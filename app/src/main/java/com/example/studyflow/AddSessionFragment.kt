package com.example.studyflow
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment

class AddSessionFragment : Fragment() {

    private lateinit var topicEditText: EditText
    private lateinit var dateEditText: EditText
    private lateinit var timeEditText: EditText
    private lateinit var saveButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_session, container, false)

        topicEditText = view.findViewById(R.id.session_topic_edit_text)
        dateEditText = view.findViewById(R.id.session_date_edit_text)
        timeEditText = view.findViewById(R.id.session_time_edit_text)
        saveButton = view.findViewById(R.id.save_session_button)

        saveButton.setOnClickListener {
            val topic = topicEditText.text.toString().trim()
            val date = dateEditText.text.toString().trim()
            val time = timeEditText.text.toString().trim()

        }

        return view
    }
}
