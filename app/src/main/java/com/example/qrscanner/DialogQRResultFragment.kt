package com.example.qrscanner

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager

class DialogQRResultFragment : DialogFragment() {
    private lateinit var resultText: String

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_qr_result, null)

        val qrResultTextView: TextView = view.findViewById(R.id.qr_result_text)
        val openLinkButton: Button = view.findViewById(R.id.open_link_button)

        qrResultTextView.text = resultText

        openLinkButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(resultText))
            startActivity(intent)
            dismiss()
        }

        builder
            .setView(view)
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }

        return builder.create()
    }

    fun show(
        fragmentManager: FragmentManager,
        tag: String?,
        resultText: String,
    ) {
        this.resultText = resultText
        super.show(fragmentManager, tag)
    }
}
