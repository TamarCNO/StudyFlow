package com.example.studyflow

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation

class DetailsFragment : Fragment() {

    private lateinit var topicTextView: TextView
    private lateinit var dateTextView: TextView
    private lateinit var timeTextView: TextView
    private lateinit var sessionImageView: ImageView
    private lateinit var editSessionButton: Button

    private var imageBytes: ByteArray? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_details, container, false)

        topicTextView = view.findViewById(R.id.sessionTopicTextView)
        dateTextView = view.findViewById(R.id.sessionDateTextView)
        timeTextView = view.findViewById(R.id.sessionTimeTextView)
        sessionImageView = view.findViewById(R.id.sessionImageView)
        editSessionButton = view.findViewById(R.id.editSessionButton)

        val args = arguments
        topicTextView.text = args?.getString("topic") ?: "No topic"
        dateTextView.text = args?.getString("date") ?: "No date"
        timeTextView.text = args?.getString("time") ?: "No time"

        imageBytes = args?.getByteArray("imageBitmap")
        if (imageBytes != null) {
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes!!.size)
            sessionImageView.setImageBitmap(bitmap)
        } else {
            sessionImageView.setImageResource(R.drawable.materials)
        }

        editSessionButton.setOnClickListener {
            val bundle = Bundle().apply {
                putString("topic", topicTextView.text.toString())
                putString("date", dateTextView.text.toString())
                putString("time", timeTextView.text.toString())
                imageBytes?.let { putByteArray("imageBitmap", it) }
            }
            Navigation.findNavController(it).navigate(
                R.id.action_detailsFragment_to_editSessionFragment,
                bundle
            )
        }

        return view
    }
}
