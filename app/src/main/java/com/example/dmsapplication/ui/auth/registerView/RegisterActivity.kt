package com.example.dmsapplication.ui.register.ui.auth.registerView

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.dmsapplication.data.repository.AuthRepository
import com.example.dmsapplication.databinding.FragmentRegisterBinding
import java.util.*

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: FragmentRegisterBinding
    private lateinit var viewModel: RegisterViewModel
    private var selectedDate: Calendar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = FragmentRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val repository = AuthRepository()
        val factory = RegisterViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[RegisterViewModel::class.java]

        setupEvents()
        observeData()
    }

    private fun setupEvents() {
        binding.tvLogin.setOnClickListener { finish() }

        binding.edtDob.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                val selected = Calendar.getInstance().apply { set(y, m, d) }
                if (selected.after(calendar)) {
                    Toast.makeText(this, "Ngày sinh không thể ở tương lai!", Toast.LENGTH_SHORT).show()
                } else {
                    selectedDate = selected
                    binding.edtDob.setText(String.format("%02d/%02d/%d", d, m + 1, y))
                }
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        binding.btnRegister.setOnClickListener {
            val name  = binding.edtName.text.toString().trim()
            val email = binding.edtEmail.text.toString().trim()
            val pass  = binding.edtPassword.text.toString().trim()
            val dob   = binding.edtDob.text.toString().trim()

            // BƯỚC 1: Kiểm tra trống
            if (name.isEmpty() || email.isEmpty() || pass.isEmpty() || selectedDate == null) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener // SAI -> DỪNG LUÔN, KHÔNG CHẠY XUỐNG DƯỚI
            }

            // BƯỚC 2: Kiểm tra định dạng Email
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.edtEmail.error = "Email không đúng định dạng"
                binding.edtEmail.requestFocus()
                return@setOnClickListener // SAI -> DỪNG LUÔN
            }

            // BƯỚC 3: Kiểm tra độ dài mật khẩu
            if (pass.length < 6) {
                binding.edtPassword.error = "Mật khẩu phải từ 6 ký tự trở lên"
                binding.edtPassword.requestFocus()
                return@setOnClickListener // SAI -> DỪNG LUÔN
            }

            // BƯỚC 4: Kiểm tra tuổi (Logic quan trọng nhất)
            if (!checkIs18Plus(selectedDate!!)) {
                Toast.makeText(this, "Bạn phải đủ 18 tuổi để đăng ký", Toast.LENGTH_LONG).show()
                return@setOnClickListener // CHƯA ĐỦ TUỔI -> DỪNG LUÔN, KHÔNG GỌI FIREBASE
            }

            // CHỈ KHI VƯỢT QUA TẤT CẢ CÁC BƯỚC TRÊN THÌ MỚI CHẠY DÒNG NÀY
            android.util.Log.d("DMS_DEBUG", "Mọi thứ OK, bắt đầu gọi ViewModel để đăng ký...")
            viewModel.register(email, pass, name, dob)
        }
    }

    private fun observeData() {
        viewModel.registerSuccess.observe(this) { success ->
            if (success == true) {
                Toast.makeText(this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        viewModel.errorMessage.observe(this) { error ->
            if (!error.isNullOrEmpty()) {
                Toast.makeText(this, "Lỗi: $error", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun checkIs18Plus(dob: Calendar): Boolean {
        val today = Calendar.getInstance()
        var age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR)
        if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) age--
        return age >= 18
    }
}