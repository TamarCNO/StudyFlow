package com.example.studyflow
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment

class EditSessionFragment : Fragment() {

    private lateinit var topicEditText: EditText
    private lateinit var dateEditText: EditText
    private lateinit var timeEditText: EditText
    private lateinit var updateButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_edit_session, container, false)

        topicEditText = view.findViewById(R.id.session_topic_edit_text)
        dateEditText = view.findViewById(R.id.session_date_edit_text)
        timeEditText = view.findViewById(R.id.session_time_edit_text)
        updateButton = view.findViewById(R.id.save_session_button)

        // קבלת נתונים לעריכה (דרך Bundle)
        val topic = arguments?.getString("topic") ?: ""
        val date = arguments?.getString("date") ?: ""
        val time = arguments?.getString("time") ?: ""

        topicEditText.setText(topic)
        dateEditText.setText(date)
        timeEditText.setText(time)

        updateButton.setOnClickListener {
            val updatedTopic = topicEditText.text.toString().trim()
            val updatedDate = dateEditText.text.toString().trim()
            val updatedTime = timeEditText.text.toString().trim()
        }

        return view
    }
}
