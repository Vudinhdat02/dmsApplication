// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Vudinhdat02

package com.example.dmsapplication.ui.settingView.profile

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.dmsapplication.databinding.FragmentProfileBinding
import java.util.*
class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ProfileViewModel
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[ProfileViewModel::class.java]

        observeViewModel()
        setupListeners()
    }
    private fun observeViewModel() {
        viewModel.userName.observe(viewLifecycleOwner) { binding.edtName.setText(it) }
        viewModel.userEmail.observe(viewLifecycleOwner) { binding.edtEmail.setText(it) }
        viewModel.userDob.observe(viewLifecycleOwner) { binding.edtDob.setText(it) }
        viewModel.updateSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(requireContext(), "Cập nhật thành công!", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
        }
        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            if (!error.isNullOrEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
            }
        }
    }
    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        binding.edtDob.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, y, m, d ->
                    binding.edtDob.setText(String.format("%02d/%02d/%d", d, m + 1, y))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
        binding.btnSave.setOnClickListener {
            val name = binding.edtName.text.toString().trim()
            val dob  = binding.edtDob.text.toString().trim()
            if (name.isEmpty()) {
                binding.edtName.error = "Tên không được để trống"
                return@setOnClickListener
            }
            viewModel.updateProfile(name, dob, null)
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}