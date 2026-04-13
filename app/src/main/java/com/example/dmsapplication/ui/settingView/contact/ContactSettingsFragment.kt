package com.example.dmsapplication.ui.settingView.contact

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView // Bổ sung import ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.dmsapplication.R
import com.example.dmsapplication.data.repository.AlertRepository
import kotlinx.coroutines.launch

class ContactSettingsFragment : Fragment(R.layout.fragment_contact_settings) {

    private val alertRepository = AlertRepository()
    private lateinit var edtEmail1: EditText
    private lateinit var edtEmail2: EditText
    private lateinit var btnSave: Button
    private lateinit var btnBack: ImageView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Ánh xạ View
        edtEmail1 = view.findViewById(R.id.edtEmail1)
        edtEmail2 = view.findViewById(R.id.edtEmail2)
        btnSave = view.findViewById(R.id.btnSaveContacts)
        btnBack = view.findViewById(R.id.btnBack)

        loadExistingContacts()

        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnSave.setOnClickListener {
            val email1 = edtEmail1.text.toString().trim()
            val email2 = edtEmail2.text.toString().trim()

            val emailsToSave = mutableListOf<String>()
            if (email1.isNotEmpty()) emailsToSave.add(email1)
            if (email2.isNotEmpty()) emailsToSave.add(email2)

            saveContacts(emailsToSave)
        }
    }

    private fun loadExistingContacts() {
        lifecycleScope.launch {
            val contacts = alertRepository.getEmergencyContacts()
            if (contacts.isNotEmpty()) edtEmail1.setText(contacts[0])
            if (contacts.size > 1) edtEmail2.setText(contacts[1])
        }
    }

    private fun saveContacts(emails: List<String>) {
        btnSave.isEnabled = false
        btnSave.text = "Đang lưu..."
        lifecycleScope.launch {
            try {
                alertRepository.saveEmergencyContacts(emails)
                Toast.makeText(requireContext(), "Đã lưu liên hệ thành công!", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Lỗi khi lưu: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                btnSave.isEnabled = true
                btnSave.text = "Lưu cấu hình"
            }
        }
    }
}