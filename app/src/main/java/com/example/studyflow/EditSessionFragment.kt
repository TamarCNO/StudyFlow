package com.example.studyflow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation

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
        topicEditText = view.findViewById(R.id.sessionTopicValue)
        dateEditText = view.findViewById(R.id.sessionDateValue)
        timeEditText = view.findViewById(R.id.sessionTimeValue)
        updateButton = view.findViewById(R.id.editSessionButton)

        val args = EditSessionFragmentArgs.fromBundle(requireArguments())
        val topic = args.topic
        val date = args.date
        val time = args.time

        topicEditText.setText(topic)
        dateEditText.setText(date)
        timeEditText.setText(time)


        updateButton.setOnClickListener {
            val updatedTopic = topicEditText.text.toString().trim()
            val updatedDate = dateEditText.text.toString().trim()
            val updatedTime = timeEditText.text.toString().trim()

            val bundle = Bundle().apply {
                putString("topic", updatedTopic)
                putString("date", updatedDate)
                putString("time", updatedTime)
            }

            Navigation.findNavController(it).navigate(R.id.action_editSessionFragment_to_detailsFragment)
        }

        return view
    }
}
