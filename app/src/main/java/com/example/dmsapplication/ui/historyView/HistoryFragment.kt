package com.example.dmsapplication.ui.historyView

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dmsapplication.R
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HistoryFragment : Fragment() {

    private val viewModel: HistoryViewModel by viewModels {
        HistoryViewModelFactory(requireActivity().application)
    }

    private lateinit var adapter: HistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_history, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = HistoryAdapter()
        view.findViewById<RecyclerView>(R.id.recyclerHistory).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@HistoryFragment.adapter
        }

        val tvTotalStats = view.findViewById<TextView>(R.id.tvTotalStats)
        val btnSelectDate = view.findViewById<MaterialButton>(R.id.btnSelectDate)
        val tvEmpty = view.findViewById<TextView>(R.id.tvEmpty)

        // Lắng nghe dữ liệu đã được LỌC THEO NGÀY
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.filteredStatsList.collect { list ->
                adapter.submitList(list)
                tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        // Lắng nghe dữ liệu TỔNG CỘNG
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.dailyTotals.collect { (drowsy, head) ->
                tvTotalStats.text = "Tổng: \nQuay đầu: $head\nNhắm mắt: $drowsy"
            }
        }

        // Format ngày hiển thị lên Nút bấm
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedDate.collect { timestamp ->
                val today = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                if (timestamp == today) {
                    btnSelectDate.text = "Hôm nay"
                } else {
                    btnSelectDate.text = dateFormat.format(timestamp)
                }
            }
        }

        // Sự kiện mở lịch chỉ cho chọn 7 ngày gần nhất
        btnSelectDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    val selectedCal = Calendar.getInstance()
                    selectedCal.set(year, month, dayOfMonth)
                    viewModel.setSelectedDate(selectedCal.timeInMillis)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )

            // Giới hạn max date là Hôm nay
            datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
            // Giới hạn min date là 7 ngày trước
            val sevenDaysAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -6) } // -6 vì bao gồm cả hôm nay là 7
            datePickerDialog.datePicker.minDate = sevenDaysAgo.timeInMillis

            datePickerDialog.show()
        }
    }
}