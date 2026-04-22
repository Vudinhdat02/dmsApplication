package com.example.dmsapplication.ui.historyView

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.dmsapplication.R
import com.example.dmsapplication.data.model.DriverStats
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class HistoryAdapter : ListAdapter<DriverStats, HistoryAdapter.ViewHolder>(DiffCallback()) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgSnapshot: ImageView = view.findViewById(R.id.imgSnapshot)
        val tvDateTime: TextView = view.findViewById(R.id.tvDateTime)
        val tvDrowsy: TextView = view.findViewById(R.id.tvDrowsyCount)
        val tvHead: TextView = view.findViewById(R.id.tvHeadCount)
        val tvSpeed: TextView = view.findViewById(R.id.tvSpeed)
        val tvSynced: TextView = view.findViewById(R.id.tvSyncStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)

        // Hiển thị ảnh — ưu tiên cloud URL, fallback local
        val imageSource: Any = when {
            item.cloudImageUrl.isNotEmpty() -> item.cloudImageUrl
            item.localImagePath.isNotEmpty() -> File(item.localImagePath)
            else -> R.drawable.ic_person
        }

        Glide.with(holder.imgSnapshot)
            .load(imageSource)
            .centerCrop()
            .placeholder(R.drawable.ic_person)
            .into(holder.imgSnapshot)

        // Ngày giờ
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        holder.tvDateTime.text = dateFormat.format(Date(item.timestamp))

        // Thống kê
        holder.tvDrowsy.text = "Nhắm mắt: ${item.drowsyCount} lần"
        holder.tvHead.text   = "Quay đầu: ${item.headDistractedCount} lần"
        holder.tvSpeed.text  = "Tốc độ: ${"%.1f".format(item.speed)} km/h"

        // Trạng thái sync
        holder.tvSynced.text = if (item.isSynced) "Đã lưu cloud" else "Chưa đồng bộ"
        holder.tvSynced.setTextColor(
            if (item.isSynced) android.graphics.Color.parseColor("#4CAF50")
            else android.graphics.Color.parseColor("#FF9800")
        )
    }

    class DiffCallback : DiffUtil.ItemCallback<DriverStats>() {
        override fun areItemsTheSame(a: DriverStats, b: DriverStats) = a.id == b.id
        override fun areContentsTheSame(a: DriverStats, b: DriverStats) = a == b
    }
}