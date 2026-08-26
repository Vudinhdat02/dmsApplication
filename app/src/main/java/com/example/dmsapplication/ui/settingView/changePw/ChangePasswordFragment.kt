// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Vudinhdat02

package com.example.dmsapplication.ui.settingView.password

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.dmsapplication.databinding.FragmentChangePasswordBinding

class ChangePasswordFragment : Fragment() {
    private var _binding: FragmentChangePasswordBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ChangePasswordViewModel
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChangePasswordBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[ChangePasswordViewModel::class.java]

        observeViewModel()
        setupListeners()
    }
    private fun observeViewModel() {
        viewModel.changeSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(requireContext(), "Đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show()
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
        binding.btnChangePassword.setOnClickListener {
            val current = binding.edtCurrentPassword.text.toString().trim()
            val newPass  = binding.edtNewPassword.text.toString().trim()
            val confirm  = binding.edtConfirmPassword.text.toString().trim()

            if (current.isEmpty() || newPass.isEmpty() || confirm.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (newPass.length < 6) {
                binding.edtNewPassword.error = "Mật khẩu phải từ 6 ký tự trở lên"
                return@setOnClickListener
            }
            if (newPass != confirm) {
                binding.edtConfirmPassword.error = "Mật khẩu xác nhận không khớp"
                return@setOnClickListener
            }
            viewModel.changePassword(current, newPass)
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}