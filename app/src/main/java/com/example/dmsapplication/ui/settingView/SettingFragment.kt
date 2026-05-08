package com.example.dmsapplication.ui.settingView

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.dmsapplication.databinding.FragmentSettingBinding
import com.example.dmsapplication.ui.OnboardingView.OnboardingActivity
import com.example.dmsapplication.ui.settingView.info.AppInfoFragment
import com.example.dmsapplication.ui.settingView.contact.ContactSettingsFragment
import com.example.dmsapplication.ui.settingView.password.ChangePasswordFragment
import com.example.dmsapplication.ui.settingView.profile.ProfileFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SettingFragment : Fragment() {

    private var _binding: FragmentSettingBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: SettingViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[SettingViewModel::class.java]
        setupListeners()
    }

    private fun setupListeners() {
        binding.btnProfile.setOnClickListener {
            navigateTo(ProfileFragment())
        }

        binding.btnChangePassword.setOnClickListener {
            navigateTo(ChangePasswordFragment())
        }

        binding.btnInfoApp.setOnClickListener {
            navigateTo(AppInfoFragment())
        }

        binding.btnSendAlert.setOnClickListener {
            navigateTo(ContactSettingsFragment())
        }

        binding.btnLogout.setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun navigateTo(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(com.example.dmsapplication.R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun showLogoutDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Đăng xuất")
            .setMessage("Bạn có chắc chắn muốn thoát khỏi hệ thống không?")
            .setCancelable(true)
            .setNegativeButton("Hủy") { dialog, _ -> dialog.dismiss() }
            .setPositiveButton("Đăng xuất") { _, _ -> performLogoutLogic() }
            .show()
    }

    private fun performLogoutLogic() {
        viewModel.logout()
        val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_first_run", true).apply()
        val intent = Intent(requireContext(), OnboardingActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        activity?.finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
