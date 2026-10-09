package ru.netology.nework.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.netology.nework.databinding.LoadStateBinding

class PagingLoadStateAdapter(
    private val retry: () -> Unit
) : LoadStateAdapter<PagingLoadStateAdapter.LoadStateViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        loadState: LoadState
    ): LoadStateViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return LoadStateViewHolder(
            LoadStateBinding.inflate(layoutInflater, parent, false),
            retry
        )
    }

    override fun onBindViewHolder(
        holder: LoadStateViewHolder,
        loadState: LoadState
    ) {
        holder.bind(loadState)
    }

    class LoadStateViewHolder(
        private val binding: LoadStateBinding,
        private val retry: () -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(loadState: LoadState) {
            binding.apply {
                progress.isVisible = loadState is LoadState.Loading
                retry.isVisible = loadState is LoadState.Error
                retry.setOnClickListener { retry() }
            }
        }
    }
}