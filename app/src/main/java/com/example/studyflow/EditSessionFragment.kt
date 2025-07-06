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

        // אתחול ה־Views לפי ה־IDs מה־layout
        topicEditText = view.findViewById(R.id.sessionTopicValue)
        dateEditText = view.findViewById(R.id.sessionDateValue)
        timeEditText = view.findViewById(R.id.sessionTimeValue)
        updateButton = view.findViewById(R.id.editSessionButton)

        // קבלת הנתונים שהועברו לפרגמנט (לעריכה)
        val args = arguments
        val topic = args?.getString("topic") ?: ""
        val date = args?.getString("date") ?: ""
        val time = args?.getString("time") ?: ""

        // הצגת הנתונים בשדות העריכה
        topicEditText.setText(topic)
        dateEditText.setText(date)
        timeEditText.setText(time)

        // מאזין לכפתור שמירה
        updateButton.setOnClickListener {
            val updatedTopic = topicEditText.text.toString().trim()
            val updatedDate = dateEditText.text.toString().trim()
            val updatedTime = timeEditText.text.toString().trim()

            // - שליחה חזרה לפרגמנט הקודם
            // - שמירה בבסיס נתונים

            // לדוגמה, נבצע חזרה עם העברת הנתונים לפרגמנט הקודם דרך Navigation עם Bundle:
            val bundle = Bundle().apply {
                putString("topic", updatedTopic)
                putString("date", updatedDate)
                putString("time", updatedTime)
            }

            // נווט חזרה (למשל לפרגמנט הפרטים) עם הנתונים המעודכנים
            Navigation.findNavController(it).navigate(R.id.action_editSessionFragment_to_detailsFragment)
        }

        return view
    }
}
