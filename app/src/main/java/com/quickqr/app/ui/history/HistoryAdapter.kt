package com.quickqr.app.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.quickqr.app.data.ScanEntity
import com.quickqr.app.databinding.ItemHistoryBinding
import com.quickqr.app.util.ContentParser
import com.quickqr.app.util.ParsedContent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter(
    private val onItemClick: (ScanEntity) -> Unit
) : ListAdapter<ScanEntity, HistoryAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("MMM d, yyyy \u00b7 h:mm a", Locale.getDefault())

        fun bind(scan: ScanEntity) {
            val preview = when (val parsed = ContentParser.parse(scan.content)) {
                is ParsedContent.Url -> parsed.url
                is ParsedContent.Wifi -> "Wi-Fi: ${parsed.ssid}"
                is ParsedContent.Email -> parsed.address
                is ParsedContent.Phone -> parsed.number
                is ParsedContent.Sms -> parsed.number
                is ParsedContent.Contact -> parsed.name ?: "Contact"
                is ParsedContent.PlainText -> parsed.text
            }
            binding.textPreview.text = preview
            binding.textTimestamp.text = dateFormat.format(Date(scan.timestamp))
            binding.root.setOnClickListener { onItemClick(scan) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ScanEntity>() {
        override fun areItemsTheSame(oldItem: ScanEntity, newItem: ScanEntity) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: ScanEntity, newItem: ScanEntity) = oldItem == newItem
    }
}
