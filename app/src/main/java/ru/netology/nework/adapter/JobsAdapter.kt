package ru.netology.nework.adapter


import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.netology.nework.R
import ru.netology.nework.databinding.CardJobBinding
import ru.netology.nework.dto.Job
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class JobsAdapter(
    private val isMyProfile: Boolean,
    private val onRemove: (Long) -> Unit,
) : ListAdapter<Job, JobsAdapter.JobViewHolder>(JobsDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobViewHolder {
        val binding = CardJobBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return JobViewHolder(binding, parent.context)
    }

    override fun onBindViewHolder(holder: JobViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class JobViewHolder(
        private val binding: CardJobBinding,
        private val context: Context
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(job: Job) {
            with(binding) {
                name.text = job.name
                val start = Instant.parse(job.start)
                    .atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault()))
                val dateText = if (job.finish == null) {
                    context.getString(R.string.date_to_present, start)
                } else {
                    val finish = Instant.parse(job.finish)
                        .atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault()))
                    context.getString(R.string.date_range, start, finish)
                }
                date.text = dateText
                position.text = job.position
                if (job.link != null) {
                    link.isVisible = true
                    link.text = job.link
                } else link.isVisible = false
                delete.isVisible = isMyProfile

                delete.setOnClickListener {
                    onRemove(job.id)
                }
            }
        }
    }

    class JobsDiffCallback : DiffUtil.ItemCallback<Job>() {
        override fun areItemsTheSame(oldItem: Job, newItem: Job): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Job, newItem: Job): Boolean {
            return oldItem == newItem
        }
    }
}