package ru.netology.nework.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
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
import ru.netology.nework.databinding.CardPostBinding
import ru.netology.nework.dto.Post
import ru.netology.nework.extensions.bindAttachment
import ru.netology.nework.extensions.formatToLocalDateTime
import ru.netology.nework.extensions.load
import ru.netology.nework.extensions.renderPlayback
import ru.netology.nework.extensions.setupMediaListeners
import ru.netology.nework.extensions.showOptionsMenu
import ru.netology.nework.media.MediaPlaybackManager
import ru.netology.nework.media.PlaybackState
import ru.netology.nework.util.DateFormatPattern
import ru.netology.nework.util.Payloads
import ru.netology.nework.util.counter

interface OnPostInteractionListener {
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
    private val onPostInteractionListener: OnPostInteractionListener
) : PagingDataAdapter<Post, PostViewHolder>(PostDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = CardPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding, playbackManager, onPostInteractionListener)
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
    private val onPostInteractionListener: OnPostInteractionListener
) : RecyclerView.ViewHolder(binding.root) {
    private var currentPost: Post? = null
    private var playbackJob: Job? = null
    private var isUserSeeking = false

    init {
        with(binding) {
            headerContainer.menu.setOnClickListener { view ->
                val post = currentPost ?: return@setOnClickListener
                view.showOptionsMenu(
                    onEdit = { onPostInteractionListener.onEdit(post) },
                    onRemove = { onPostInteractionListener.onRemove(post) }
                )
            }
            attachmentContainer.setupMediaListeners(
                onVideoClick = { currentPost?.let(onPostInteractionListener::onVideo) },
                onAudioClick = { currentPost?.let(onPostInteractionListener::onAudio) },
                onSeekStart = { isUserSeeking = true },
                onSeekStop = { progress ->
                    isUserSeeking = false
                    currentPost?.let { post -> onPostInteractionListener.onSeek(post, progress) }
                }
            )
            postContentContainer.setOnClickListener { currentPost?.let(onPostInteractionListener::onPost) }
            share.setOnClickListener { currentPost?.let(onPostInteractionListener::onShare) }
            like.setOnClickListener {
                currentPost?.let { post ->
                    onPostInteractionListener.onLike(post)
                    like.isChecked = post.likedByMe
                }
            }
        }
    }

    fun bind(post: Post) {
        currentPost = post
        with(binding) {
            headerContainer.avatar.load(post.authorAvatar, true)
            headerContainer.author.text = post.author
            headerContainer.publishedOrJob.text =
                post.published.formatToLocalDateTime(DateFormatPattern.DATE_TIME)
            headerContainer.menu.isVisible = post.ownedByMe
            content.isVisible = post.content.isNotBlank()
            content.text = post.content
            like.isChecked = post.likedByMe
            like.text = counter(post.likeOwnerIds.size.toLong())
            attachmentContainer.bindAttachment(post.attachment)
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
            like.text = counter(post.likeOwnerIds.size.toLong())
        }
    }

    private fun renderPlaybackState(state: PlaybackState) {
        val post = currentPost ?: return
        binding.attachmentContainer.renderPlayback(
            type = post.attachment?.type,
            isActive = state.currentId == post.id,
            state = state,
            exoPlayer = playbackManager.exoPlayer,
            isUserSeeking = isUserSeeking
        )
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