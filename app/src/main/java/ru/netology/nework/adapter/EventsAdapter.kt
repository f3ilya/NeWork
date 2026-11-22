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
import ru.netology.nework.databinding.CardEventBinding
import ru.netology.nework.dto.Event
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

interface OnEventInteractionListener {
    fun onLike(event: Event)
    fun onParticipant(event: Event)
    fun onShare(event: Event)
    fun onAudio(event: Event)
    fun onVideo(event: Event)
    fun onSeek(event: Event, progress: Long)
    fun onEvent(event: Event)
    fun onRemove(event: Event)
    fun onEdit(event: Event)
}

class EventsAdapter(
    private val playbackManager: MediaPlaybackManager,
    private val onEventInteractionListener: OnEventInteractionListener
) : PagingDataAdapter<Event, EventViewHolder>(EventDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = CardEventBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EventViewHolder(binding, playbackManager, onEventInteractionListener)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        getItem(position)?.let { event ->
            holder.bind(event)
        }
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int, payloads: List<Any?>) {
        when {
            payloads.contains(Payloads.LIKE) -> {
                val event = getItem(position) ?: return
                holder.updateLikeState(event)
            }

            payloads.contains(Payloads.PARTICIPANT) -> {
                val event = getItem(position) ?: return
                holder.updateParticipantState(event)
            }

            else -> super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun onViewAttachedToWindow(holder: EventViewHolder) {
        super.onViewAttachedToWindow(holder)
        holder.onAttach()
    }

    override fun onViewDetachedFromWindow(holder: EventViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.onDetach()
    }

    override fun onViewRecycled(holder: EventViewHolder) {
        super.onViewRecycled(holder)
        holder.clear()
    }
}

class EventViewHolder(
    private val binding: CardEventBinding,
    private val playbackManager: MediaPlaybackManager,
    private val onEventInteractionListener: OnEventInteractionListener
) : RecyclerView.ViewHolder(binding.root) {
    private var currentEvent: Event? = null
    private var playbackJob: Job? = null
    private var isUserSeeking = false

    init {
        with(binding) {
            headerContainer.menu.setOnClickListener { view ->
                val event = currentEvent ?: return@setOnClickListener
                view.showOptionsMenu(
                    onEdit = { onEventInteractionListener.onEdit(event) },
                    onRemove = { onEventInteractionListener.onRemove(event) }
                )
            }
            attachmentContainer.setupMediaListeners(
                onVideoClick = { currentEvent?.let(onEventInteractionListener::onVideo) },
                onAudioClick = { currentEvent?.let(onEventInteractionListener::onAudio) },
                onSeekStart = { isUserSeeking = true },
                onSeekStop = { progress ->
                    isUserSeeking = false
                    currentEvent?.let { event ->
                        onEventInteractionListener.onSeek(
                            event,
                            progress
                        )
                    }
                }
            )
            eventContainer.setOnClickListener { currentEvent?.let(onEventInteractionListener::onEvent) }
            share.setOnClickListener { currentEvent?.let(onEventInteractionListener::onShare) }
            like.setOnClickListener {
                currentEvent?.let { event ->
                    onEventInteractionListener.onLike(event)
                    like.isChecked = event.likedByMe
                }
            }
            participant.setOnClickListener {
                currentEvent?.let { event ->
                    onEventInteractionListener.onParticipant(event)
                    participant.isChecked = event.participatedByMe
                }
            }
        }
    }

    fun bind(event: Event) {
        currentEvent = event
        with(binding) {
            headerContainer.avatar.load(event.authorAvatar, true)
            headerContainer.author.text = event.author
            headerContainer.publishedOrJob.text =
                event.published.formatToLocalDateTime(DateFormatPattern.DATE_TIME)
            headerContainer.menu.isVisible = event.ownedByMe
            type.text = event.type.toString()
            datetime.text = event.datetime.formatToLocalDateTime(DateFormatPattern.DATE_TIME)
            content.isVisible = event.content.isNotBlank()
            content.text = event.content
            like.isChecked = event.likedByMe
            like.text = counter(event.likeOwnerIds.size.toLong())
            participant.isChecked = event.participatedByMe
            participant.text = counter(event.participantsIds.size.toLong())
            attachmentContainer.bindAttachment(event.attachment)
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
        currentEvent = null
        binding.attachmentContainer.videoAttachment.player = null
    }

    fun updateLikeState(event: Event) {
        currentEvent = event
        with(binding) {
            like.isChecked = event.likedByMe
            like.text = counter(event.likeOwnerIds.size.toLong())
        }
    }

    fun updateParticipantState(event: Event) {
        currentEvent = event
        with(binding) {
            participant.isChecked = event.participatedByMe
            participant.text = counter(event.participantsIds.size.toLong())
        }
    }

    private fun renderPlaybackState(state: PlaybackState) {
        val event = currentEvent ?: return
        binding.attachmentContainer.renderPlayback(
            type = event.attachment?.type,
            isActive = state.currentId == event.id,
            state = state,
            exoPlayer = playbackManager.exoPlayer,
            isUserSeeking = isUserSeeking
        )
    }
}

class EventDiffCallback : DiffUtil.ItemCallback<Event>() {
    override fun areItemsTheSame(oldItem: Event, newItem: Event): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Event, newItem: Event): Boolean {
        return oldItem == newItem
    }

    override fun getChangePayload(oldItem: Event, newItem: Event): Any? {
        return when {
            oldItem.likedByMe != newItem.likedByMe
                    || oldItem.likeOwnerIds.size != newItem.likeOwnerIds.size -> Payloads.LIKE

            oldItem.participatedByMe != newItem.participatedByMe
                    || oldItem.participantsIds.size != newItem.participantsIds.size -> Payloads.PARTICIPANT

            else -> super.getChangePayload(oldItem, newItem)
        }
    }
}