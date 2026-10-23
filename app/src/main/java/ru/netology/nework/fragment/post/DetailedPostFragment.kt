package ru.netology.nework.fragment.post

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import dev.androidbroadcast.vbpd.viewBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.netology.nework.R
import ru.netology.nework.databinding.FragmentDetailedPostBinding
import ru.netology.nework.dto.AttachmentType
import ru.netology.nework.dto.Post
import ru.netology.nework.extensions.load
import ru.netology.nework.extensions.renderAvatarStack
import ru.netology.nework.map.YandexMapManager
import ru.netology.nework.media.MediaPlaybackManager
import ru.netology.nework.model.AvatarModel
import ru.netology.nework.viewmodel.post.DetailedPostViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@AndroidEntryPoint
class DetailedPostFragment : Fragment(R.layout.fragment_detailed_post) {
    private val binding by viewBinding(FragmentDetailedPostBinding::bind)
    private val viewModel: DetailedPostViewModel by activityViewModels()
    private val args: DetailedPostFragmentArgs by navArgs()

    @Inject
    lateinit var playbackManager: MediaPlaybackManager

    @Inject
    lateinit var yandexMapManager: YandexMapManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.setDetailedPostId(args.postId)
        setupYandexMap()
        setupExoPlayer()
        observeViewModel()
    }

    private fun setupYandexMap() {
        yandexMapManager.bindMapView(binding.map)
        viewLifecycleOwner.lifecycle.addObserver(yandexMapManager)
    }

    private fun setupExoPlayer() {
        viewLifecycleOwner.lifecycle.addObserver(playbackManager)
        playbackManager.setOnErrorListener { error ->
            viewModel.handleError(error)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.detailedPost.collectLatest { post ->
                        post?.let {
                            bindPost(post)
                            if (it.coordinates != null && !it.coordinates.lat.isNaN() && !it.coordinates.long.isNaN()) {
                                binding.map.isVisible = true
                                yandexMapManager.showPoint(
                                    it.coordinates.lat,
                                    it.coordinates.long
                                )
                                yandexMapManager.disableAllGestures()
                            } else {
                                binding.map.isVisible = false
                            }
                        }
                    }
                }

                launch {
                    viewModel.state.collectLatest { state ->
                        binding.postContentContainer.isVisible = !state.loading

                        if (state.error && state.errorMessage != null) {
                            error(state.errorMessage)
                            viewModel.resetState()
                        }
                    }
                }
            }
        }
    }

    private fun bindPost(post: Post) {
        binding.apply {
            if (!post.authorAvatar.isNullOrBlank()) {
                authorAvatar.load(post.authorAvatar, true)
            } else {
                authorAvatar.setImageResource(R.drawable.ic_avatar_placeholder_48)
            }
            authorName.text = post.author
            authorJob.text = if (post.authorJob.isNullOrBlank()) {
                getString(R.string.looking_for_a_job)
            } else post.authorJob
            content.isVisible = if (post.content.isNotBlank() || post.content.isNotEmpty()) {
                content.text = post.content
                true
            } else false
            published.text = Instant.parse(post.published)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
            like.isChecked = post.likedByMe
            like.text = post.likeOwnerIds.size.toString()

            attachmentContainer.imageAttachment.isVisible = false
            attachmentContainer.videoContainer.isVisible = false
            attachmentContainer.audioAttachment.isVisible = false

            val isThisPlaying = playbackManager.currentPlayingPostId == post.id

            when (post.attachment?.type) {
                AttachmentType.IMAGE -> {
                    if (post.attachment.url.isNotEmpty()) {
                        attachmentContainer.imageAttachment.load(post.attachment.url)
                        attachmentContainer.imageAttachment.isVisible = true
                    }
                }

                AttachmentType.VIDEO -> {
                    attachmentContainer.videoContainer.isVisible = true
                    if (isThisPlaying) {
                        playbackManager.rebindVideo(
                            attachmentContainer.videoAttachment,
                            attachmentContainer.videoPlaceholder
                        )
                    } else {
                        attachmentContainer.videoPlaceholder.isVisible = true
                        attachmentContainer.videoAttachment.isVisible = false
                        if (attachmentContainer.videoAttachment.player != null) {
                            attachmentContainer.videoAttachment.player = null
                        }
                    }
                    attachmentContainer.videoContainer.setOnClickListener {
                        playbackManager.playVideo(
                            postId = post.id,
                            url = post.attachment.url,
                            playerView = attachmentContainer.videoAttachment,
                            placeholder = attachmentContainer.videoPlaceholder
                        )
                    }
                }

                AttachmentType.AUDIO -> {
                    attachmentContainer.audioAttachment.isVisible = true

                    if (isThisPlaying) {
                        playbackManager.rebindAudio(
                            attachmentContainer.audioSeekBar,
                            attachmentContainer.audioPlayPause
                        )
                    } else {
                        attachmentContainer.audioPlayPause.isChecked = false
                        attachmentContainer.audioSeekBar.progress = 0
                    }

                    attachmentContainer.audioPlayPause.setOnClickListener {
                        playbackManager.playAudio(
                            postId = post.id,
                            url = post.attachment.url,
                            seekBar = attachmentContainer.audioSeekBar,
                            playButton = attachmentContainer.audioPlayPause
                        )
                    }
                }

                else -> {
                    attachmentContainer.audioAttachment.isVisible = false
                    attachmentContainer.videoAttachment.isVisible = false
                    attachmentContainer.imageAttachment.isVisible = false
                }
            }

            like.setOnClickListener {
                viewModel.likePost(id = post.id, likeByMe = post.likedByMe)
                like.isChecked = post.likedByMe
            }

            val likeModels = post.likeOwnerIds.map { userId ->
                AvatarModel(
                    userId = userId,
                    avatarUrl = post.users[userId.toString()]?.avatar
                )
            }

            avatarLikersStack.renderAvatarStack(
                models = likeModels,
                moreButton = moreLikes,
                onAvatarClickListener = { userId ->
//                    val action = DetailedPostFragmentDirections.actionDetailedPostFragmentToLikeOwnersFragment(post.id)
//                    findNavController().navigate(action)
                    TODO()
                },
                onMoreClickListener = {
//                    val action = DetailedPostFragmentDirections.actionDetailedPostFragmentToLikeOwnersFragment(post.id)
//                    findNavController().navigate(action)
                    TODO()
                }
            )

            if (post.mentionIds.isNotEmpty()) {
                mentionedContainer.root.isVisible = true
                mentionedContainer.mentionedIcon.text = post.mentionIds.size.toString()
                val mentionModels = post.mentionIds.map { userId ->
                    AvatarModel(
                        userId = userId,
                        avatarUrl = post.users[userId.toString()]?.avatar
                    )
                }
                mentionedContainer.avatarMentionedStack.renderAvatarStack(
                    models = mentionModels,
                    moreButton = mentionedContainer.moreMentioned,
                    onAvatarClickListener = { userId ->
//                    val action = DetailedPostFragmentDirections.actionDetailedPostFragmentToLikeOwnersFragment(post.id)
//                    findNavController().navigate(action)
                        TODO()
                    },
                    onMoreClickListener = {
//                    val action = DetailedPostFragmentDirections.actionDetailedPostFragmentToLikeOwnersFragment(post.id)
//                    findNavController().navigate(action)
                        TODO()
                    }
                )
            } else mentionedContainer.root.isVisible = false

            menu.isVisible = post.ownedByMe
            menu.setOnClickListener {
                PopupMenu(it.context, it).apply {
                    inflate(R.menu.menu_options)
                    setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            R.id.remove -> {
                                viewModel.removePost(post.id)
                                findNavController().navigateUp()
                                true
                            }

                            R.id.edit -> {
                                TODO()
                                true
                            }

                            else -> false
                        }
                    }
                }.show()
            }
        }
    }

    private fun error(error: String) {
        val messageResId = when (error) {
            "error_playback" -> R.string.error_playback
            "error_file_too_large" -> R.string.error_file_too_large
            "error_file_access" -> R.string.error_file_access
            "error_network" -> R.string.error_network
            "error_db" -> R.string.error_db
            "403" -> R.string.authorization_is_required_for_this_action
            "404" -> R.string.post_not_found
            else -> R.string.error_unknown
        }

        Snackbar.make(
            binding.root,
            getString(messageResId),
            Snackbar.LENGTH_LONG
        ).show()
    }
}