package ru.netology.nework.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import ru.netology.nework.R
import ru.netology.nework.databinding.CardPostBinding
import ru.netology.nework.dto.AttachmentType
import ru.netology.nework.dto.Post
import ru.netology.nework.extensions.load
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

interface OnInteractionListener {
    fun onLike(post: Post)
    fun onShare(post: Post)
    fun onAudio(post: Post, isPlaying: Boolean)
    fun onVideo(post: Post)
    fun onPost(post: Post)
}

class PostsAdapter(
    private val onInteractionListener: OnInteractionListener
) : PagingDataAdapter<Post, PostViewHolder>(PostDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = CardPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding, onInteractionListener)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        getItem(position)?.let { post ->
            holder.bind(post)
        }
    }

}

class PostViewHolder(
    private val binding: CardPostBinding,
    private val onInteractionListener: OnInteractionListener
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(post: Post) {
        binding.apply {
            if (!post.authorAvatar.isNullOrBlank()) {
                avatar.load(post.authorAvatar, true)
            } else {
                avatar.setImageResource(R.drawable.ic_avatar_placeholder_48)
            }
            author.text = post.author
            content.text = post.content
            published.text = Instant.parse(post.published)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
            like.isChecked = post.likedByMe
            like.text = post.likeOwnerIds.size.toString()
            when (post.attachment?.type) {
                AttachmentType.IMAGE -> {
                    if (!post.attachment.url.isEmpty()) {
                        imageAttachment.load(post.attachment.url)
                        imageAttachment.isVisible = true
                    } else {
                        imageAttachment.isVisible = false
                    }
                }

                AttachmentType.VIDEO -> {
                    videoAttachment.isVisible = true
                    videoAttachment.setOnClickListener {
                        onInteractionListener.onVideo(post)
                    }
                }

                AttachmentType.AUDIO -> {
                    audioAttachment.isVisible = true
                    audioAttachment.setOnClickListener {
                        val isPlaying = audioAttachment.isChecked
                        onInteractionListener.onAudio(post, isPlaying)
                    }
                }

                else -> {
                    audioAttachment.isVisible = false
                    videoAttachment.isVisible = false
                    imageAttachment.isVisible = false
                }
            }

            share.setOnClickListener { onInteractionListener.onShare(post) }
            content.setOnClickListener { onInteractionListener.onPost(post) }
            like.setOnClickListener {
                onInteractionListener.onLike(post)
                like.isChecked = post.likedByMe
            }
        }
    }
}

class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
    override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean {
        return oldItem == newItem
    }

}