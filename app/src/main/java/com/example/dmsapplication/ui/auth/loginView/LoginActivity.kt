package com.example.dmsapplication.ui.auth.loginView
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.dmsapplication.data.repository.AuthRepository
import com.example.dmsapplication.databinding.FragmentLoginBinding
import com.example.dmsapplication.ui.HomeActivity
import com.example.dmsapplication.ui.auth.registerView.RegisterActivity
import com.google.firebase.auth.FirebaseAuth
class LoginActivity : AppCompatActivity() {
    private lateinit var binding: FragmentLoginBinding
    private lateinit var viewModel: LoginViewModel
    private val auth = FirebaseAuth.getInstance()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (auth.currentUser != null) {
            navigateToHome()
            return
        }
        binding = FragmentLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val repository = AuthRepository(this)
        val factory = LoginViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[LoginViewModel::class.java]
        setupEvents()
        observeData()
    }
    private fun setupEvents() {
        binding.btnLogin.setOnClickListener {
            val email = binding.edtAcc.text.toString().trim()
            val pass = binding.edtPass.text.toString().trim()
            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.edtAcc.error = "Định dạng email không hợp lệ"
                return@setOnClickListener
            }
            viewModel.loginWithEmail(email, pass)
        }
        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        binding.tvForgetPass.setOnClickListener {
            binding.layoutInputAccount.visibility = View.GONE
            binding.layoutForgetPass.root.visibility = View.VISIBLE
        }
        binding.layoutForgetPass.tvBackToLogin.setOnClickListener {
            binding.layoutForgetPass.root.visibility = View.GONE
            binding.layoutInputAccount.visibility = View.VISIBLE
        }
        binding.layoutForgetPass.btnVerify.setOnClickListener {
            val email = binding.layoutForgetPass.edtAcc.text.toString().trim()
            if (email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                viewModel.forgotPassword(email)
            } else {
                Toast.makeText(this, "Vui lòng nhập Email hợp lệ", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun observeData() {
        viewModel.loginSuccess.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
                navigateToHome()
            }
        }
        viewModel.errorMessage.observe(this) { error ->
            if (!error.isNullOrEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
            }
        }
        viewModel.isResetSent.observe(this) { sent ->
            if (sent) {
                Toast.makeText(this, "Đã gửi mail khôi phục mật khẩu!", Toast.LENGTH_LONG).show()
                binding.layoutForgetPass.root.visibility = View.GONE
                binding.layoutInputAccount.visibility = View.VISIBLE
            }
        }
    }
    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
