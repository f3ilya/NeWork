package ru.netology.nework.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.netology.nework.R
import ru.netology.nework.databinding.CardPostBinding
import ru.netology.nework.dto.AttachmentType
import ru.netology.nework.dto.Post
import ru.netology.nework.extensions.load
import ru.netology.nework.media.MediaPlaybackManager
import ru.netology.nework.media.PlaybackState
import ru.netology.nework.util.Payloads
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

interface OnInteractionListener {
    fun onLike(post: Post)
    fun onShare(post: Post)
    fun onAudio(post: Post)
    fun onVideo(post: Post)
    fun onSeek(post: Post, progress: Long)
    fun onPost(post: Post)
    fun onRemove(post: Post)
    fun onEdit(post: Post)
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

    override fun onBindViewHolder(holder: PostViewHolder, position: Int, payloads: List<Any?>) {
        if (payloads.contains(Payloads.LIKE)) {
            val post = getItem(position) ?: return
            holder.updateLikeState(post)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun onViewAttachedToWindow(holder: PostViewHolder) {
        super.onViewAttachedToWindow(holder)
        holder.onAttach()
    }

    override fun onViewDetachedFromWindow(holder: PostViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.onDetach()
    }

    override fun onViewRecycled(holder: PostViewHolder) {
        super.onViewRecycled(holder)
        holder.clear()
    }
}

class PostViewHolder(
    private val binding: CardPostBinding,
    private val playbackManager: MediaPlaybackManager,
    private val onInteractionListener: OnInteractionListener
) : RecyclerView.ViewHolder(binding.root) {
    private var currentPost: Post? = null
    private var playbackJob: Job? = null
    private var isUserSeeking = false

    init {
        with(binding) {
            share.setOnClickListener { currentPost?.let(onInteractionListener::onShare) }
            postContentContainer.setOnClickListener { currentPost?.let(onInteractionListener::onPost) }
            like.setOnClickListener {
                currentPost?.let { post ->
                    onInteractionListener.onLike(post)
                    like.isChecked = post.likedByMe
                }
            }
            menu.setOnClickListener {
                val post = currentPost ?: return@setOnClickListener
                PopupMenu(it.context, it).apply {
                    inflate(R.menu.menu_options)
                    setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            R.id.remove -> {
                                onInteractionListener.onRemove(post)
                                true
                            }

                            R.id.edit -> {
                                onInteractionListener.onEdit(post)
                                true
                            }

                            else -> false
                        }
                    }
                }.show()
            }

            attachmentContainer.videoContainer.setOnClickListener {
                currentPost?.let(onInteractionListener::onVideo)
            }

            attachmentContainer.audioPlayPause.setOnClickListener {
                currentPost?.let(onInteractionListener::onAudio)
            }

            attachmentContainer.audioSeekBar.setOnSeekBarChangeListener(object :
                SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    isUserSeeking = true
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    isUserSeeking = false
                    val post = currentPost ?: return
                    seekBar?.let { onInteractionListener.onSeek(post, it.progress.toLong()) }
                }
            })
        }
    }

    fun bind(post: Post) {
        currentPost = post
        binding.apply {
            if (!post.authorAvatar.isNullOrBlank()) {
                avatar.load(post.authorAvatar, true)
            } else {
                avatar.setImageResource(R.drawable.ic_avatar_placeholder_48)
            }
            author.text = post.author
            if (post.content.isBlank() || post.content.isEmpty()) {
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
            menu.isVisible = post.ownedByMe

            val type = post.attachment?.type
            attachmentContainer.imageAttachment.isVisible = type == AttachmentType.IMAGE
            attachmentContainer.videoContainer.isVisible = type == AttachmentType.VIDEO
            attachmentContainer.audioAttachment.isVisible = type == AttachmentType.AUDIO

            if (type == AttachmentType.IMAGE) {
                attachmentContainer.imageAttachment.load(post.attachment.url)
            }
        }
    }

    fun onAttach() {
        val lifecycleOwner = binding.root.findViewTreeLifecycleOwner() ?: return
        playbackJob?.cancel()
        playbackJob = lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                playbackManager.playbackState.collectLatest { state ->
                    renderPlaybackState(state)
                }
            }
        }
    }

    fun onDetach() {
        playbackJob?.cancel()
        playbackJob = null
        binding.attachmentContainer.videoAttachment.player = null
    }

    fun clear() {
        currentPost = null
        binding.attachmentContainer.videoAttachment.player = null
    }

    fun updateLikeState(post: Post) {
        currentPost = post
        with(binding) {
            like.isChecked = post.likedByMe
            like.text = post.likeOwnerIds.size.toString()
        }
    }

    private fun renderPlaybackState(state: PlaybackState) {
        val post = currentPost ?: return
        val isActive = (state.currentPostId == post.id)

        with(binding.attachmentContainer) {
            when (post.attachment?.type) {
                AttachmentType.AUDIO -> {
                    audioPlayPause.isChecked = isActive && state.isPlaying
                    audioSeekBar.max = if (isActive) state.duration.toInt() else 0
                    if (!isUserSeeking) {
                        audioSeekBar.progress = if (isActive) state.progress.toInt() else 0
                    }
                }

                AttachmentType.VIDEO -> {
                    videoPlaceholder.isVisible = !isActive
                    videoAttachment.isVisible = isActive
                    videoAttachment.player = if (isActive) playbackManager.exoPlayer else null
                }

                else -> Unit
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

    override fun getChangePayload(oldItem: Post, newItem: Post): Any? {
        return if (oldItem.likedByMe != newItem.likedByMe || oldItem.likeOwnerIds.size != newItem.likeOwnerIds.size) {
            Payloads.LIKE
        } else {
            super.getChangePayload(oldItem, newItem)
        }
    }
}