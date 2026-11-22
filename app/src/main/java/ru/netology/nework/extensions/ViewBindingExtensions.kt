package ru.netology.nework.extensions

import android.widget.SeekBar
import androidx.core.view.isVisible
import androidx.media3.exoplayer.ExoPlayer
import ru.netology.nework.databinding.LayoutAttachmentContainerBinding
import ru.netology.nework.databinding.LayoutUserSectionContainerBinding
import ru.netology.nework.dto.Attachment
import ru.netology.nework.dto.AttachmentType
import ru.netology.nework.dto.UserPreview
import ru.netology.nework.dto.UserResponse
import ru.netology.nework.media.PlaybackState
import ru.netology.nework.model.AvatarModel
import ru.netology.nework.util.FragmentMode
import ru.netology.nework.util.counter

fun LayoutAttachmentContainerBinding.setupMediaListeners(
    onVideoClick: () -> Unit,
    onAudioClick: () -> Unit,
    onSeekStart: () -> Unit,
    onSeekStop: (Long) -> Unit
) {
    videoContainer.setOnClickListener { onVideoClick() }
    audioPlayPause.setOnClickListener { onAudioClick() }
    audioSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {}

        override fun onStartTrackingTouch(seekBar: SeekBar?) {
            onSeekStart()
        }

        override fun onStopTrackingTouch(seekBar: SeekBar?) {
            val progress = seekBar?.progress?.toLong() ?: return
            onSeekStop(progress)
        }
    })
}

fun LayoutAttachmentContainerBinding.bindAttachment(attachment: Attachment?) {
    val type = attachment?.type
    imageAttachment.isVisible = type == AttachmentType.IMAGE
    videoContainer.isVisible = type == AttachmentType.VIDEO
    audioAttachment.isVisible = type == AttachmentType.AUDIO

    if (type == AttachmentType.IMAGE) {
        imageAttachment.load(attachment.url)
    }
}

fun LayoutAttachmentContainerBinding.renderPlayback(
    type: AttachmentType?,
    isActive: Boolean,
    state: PlaybackState,
    exoPlayer: ExoPlayer?,
    isUserSeeking: Boolean
) {
    when (type) {
        AttachmentType.VIDEO -> {
            videoPlaceholder.isVisible = !isActive
            videoAttachment.isVisible = isActive
            videoAttachment.player = if (isActive) exoPlayer else null
        }

        AttachmentType.AUDIO -> {
            audioPlayPause.isChecked = isActive && state.isPlaying
            audioSeekBar.max = if (isActive) state.duration.toInt() else 0
            if (!isUserSeeking) {
                audioSeekBar.progress = if (isActive) state.progress.toInt() else 0
            }
        }

        else -> Unit
    }
}

fun LayoutUserSectionContainerBinding.bindUserSection(
    mode: FragmentMode,
    userIds: List<Long>,
    usersMap: Map<String, UserPreview>,
    isChecked: Boolean = false,
    onAvatarClick: (Long) -> Unit,
    onMoreClick: (FragmentMode, LongArray) -> Unit
) {
    val models = userIds.map { id ->
        AvatarModel(userId = id, avatarUrl = usersMap[id.toString()]?.avatar)
    }
    renderSection(mode, models, isChecked, onAvatarClick) {
        onMoreClick(mode, userIds.toLongArray())
    }
}

fun LayoutUserSectionContainerBinding.bindUserSection(
    mode: FragmentMode,
    users: List<UserResponse>,
    onAvatarClick: (Long) -> Unit,
    onMoreClick: (FragmentMode, LongArray) -> Unit
) {
    val models = users.map { user ->
        AvatarModel(userId = user.id, avatarUrl = user.avatar)
    }
    renderSection(mode, models, isChecked = false, onAvatarClick) {
        onMoreClick(mode, users.map { it.id }.toLongArray())
    }
}

private fun LayoutUserSectionContainerBinding.renderSection(
    mode: FragmentMode,
    models: List<AvatarModel>,
    isChecked: Boolean,
    onAvatarClick: (Long) -> Unit,
    onMoreClick: () -> Unit
) {
    if (mode.hideIfEmpty && models.isEmpty()) {
        root.isVisible = false
        return
    }
    root.isVisible = true
    sectionTitle.setText(mode.titleRes)
    if (mode.iconRes != null) {
        actionButton.apply {
            isVisible = true
            setIconResource(mode.iconRes)
            text = counter(models.size.toLong())
            isCheckable = mode.isActionCheckable
            isClickable = mode.isActionCheckable
            this.isChecked = mode.isActionCheckable && isChecked
        }
    } else {
        actionButton.isVisible = false
    }

    avatarStack.renderAvatarStack(
        models = models,
        moreButton = moreButton,
        onAvatarClickListener = onAvatarClick,
        onMoreClickListener = onMoreClick
    )
}