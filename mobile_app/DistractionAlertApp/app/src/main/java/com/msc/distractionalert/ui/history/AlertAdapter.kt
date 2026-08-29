package com.msc.distractionalert.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.msc.distractionalert.data.local.PrefsManager
import com.msc.distractionalert.data.model.Alert
import com.msc.distractionalert.databinding.ItemAlertBinding
import com.msc.distractionalert.util.DistractionClasses
import com.msc.distractionalert.util.formatTimestamp
import java.util.Locale

class AlertAdapter(private val prefsManager: PrefsManager) : ListAdapter<Alert, AlertAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAlertBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, prefsManager)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemAlertBinding,
        private val prefsManager: PrefsManager
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(alert: Alert) {
            binding.classNameText.text = DistractionClasses.displayName(alert.className)
            binding.timestampText.text = formatTimestamp(alert.timestamp)
            binding.confidenceText.text = String.format(Locale.US, "%.0f%%", alert.confidence * 100)

            if (alert.hasImage) {
                // GET /api/alerts/{id}/image needs the same bearer token as every
                // other endpoint - Glide's default loader has no way to attach one,
                // so wrap the URL with an auth header via GlideUrl/LazyHeaders.
                val imageUrl = "${prefsManager.serverUrl}/api/alerts/${alert.id}/image"
                val glideUrl = GlideUrl(
                    imageUrl,
                    LazyHeaders.Builder()
                        .addHeader("Authorization", "Bearer ${prefsManager.authToken}")
                        .build()
                )
                binding.thumbnailImage.visibility = View.VISIBLE
                Glide.with(binding.thumbnailImage)
                    .load(glideUrl)
                    .centerCrop()
                    .into(binding.thumbnailImage)
            } else {
                binding.thumbnailImage.visibility = View.GONE
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Alert>() {
            override fun areItemsTheSame(oldItem: Alert, newItem: Alert) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Alert, newItem: Alert) = oldItem == newItem
        }
    }
}
