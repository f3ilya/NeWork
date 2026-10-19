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
import ru.netology.nework.media.MediaPlaybackManager
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

interface OnInteractionListener {
    fun onLike(post: Post)
    fun onShare(post: Post)
    fun onAudio(post: Post, binding: CardPostBinding)
    fun onVideo(post: Post, binding: CardPostBinding)
    fun onPost(post: Post)
}

class PostsAdapter(
    private val playbackManager: MediaPlaybackManager,
    private val onInteractionListener: OnInteractionListener
) : PagingDataAdapter<Post, PostViewHolder>(PostDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = CardPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding, playbackManager, onInteractionListener)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        getItem(position)?.let { post ->
            holder.bind(post)
        }
    }

}

class PostViewHolder(
    private val binding: CardPostBinding,
    private val playbackManager: MediaPlaybackManager,
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
            if (post.content.isBlank() || post.content.isEmpty()){
                content.isVisible = false
            } else {
                content.isVisible = true
                content.text = post.content
            }
            published.text = Instant.parse(post.published)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
            like.isChecked = post.likedByMe
            like.text = post.likeOwnerIds.size.toString()

            imageAttachment.isVisible = false
            videoContainer.isVisible = false
            audioAttachment.isVisible = false

            val isThisPlaying = playbackManager.currentPlayingPostId == post.id

            when (post.attachment?.type) {
                AttachmentType.IMAGE -> {
                    if (post.attachment.url.isNotEmpty()) {
                        imageAttachment.load(post.attachment.url)
                        imageAttachment.isVisible = true
                    }
                }

                AttachmentType.VIDEO -> {
                    videoContainer.isVisible = true
                    if (isThisPlaying) {
                        playbackManager.rebindVideo(videoAttachment, videoPlaceholder)
                    } else {
                        videoPlaceholder.isVisible = true
                        videoAttachment.isVisible = false
                        if (videoAttachment.player != null) {
                            videoAttachment.player = null
                        }
                    }
                    videoContainer.setOnClickListener {
                        onInteractionListener.onVideo(post, binding)
                    }
                }

                AttachmentType.AUDIO -> {
                    audioAttachment.isVisible = true

                    if (isThisPlaying) {
                        playbackManager.rebindAudio(audioSeekBar, audioPlayPause)
                    } else {
                        audioPlayPause.isChecked = false
                        audioSeekBar.progress = 0
                    }

                    audioPlayPause.setOnClickListener {
                        onInteractionListener.onAudio(post, binding)
                    }
                }

                else -> {
                    audioAttachment.isVisible = false
                    videoAttachment.isVisible = false
                    imageAttachment.isVisible = false
                }
            }

            share.setOnClickListener { onInteractionListener.onShare(post) }
            postContentContainer.setOnClickListener { onInteractionListener.onPost(post) }
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