package ru.netology.nework.fragment

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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
import ru.netology.nework.viewmodel.PostViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@AndroidEntryPoint
class DetailedPostFragment : Fragment(R.layout.fragment_detailed_post) {
    private val binding by viewBinding(FragmentDetailedPostBinding::bind)
    private val viewModel: PostViewModel by activityViewModels()
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
        playbackManager.setOnErrorListener { errorMessage ->
            error(errorMessage)
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
                                yandexMapManager.disableAllGestures(binding.map)
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
            authorJob.text = post.authorJob
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
                        playbackManager.playVideo(
                            postId = post.id,
                            url = post.attachment.url,
                            playerView = videoAttachment,
                            placeholder = videoPlaceholder
                        )
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
                        playbackManager.playAudio(
                            postId = post.id,
                            url = post.attachment.url,
                            seekBar = audioSeekBar,
                            playButton = audioPlayPause
                        )
                    }
                }

                else -> {
                    audioAttachment.isVisible = false
                    videoAttachment.isVisible = false
                    imageAttachment.isVisible = false
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
                mentionedContainer.isVisible = true
                mentionedIcon.text = post.mentionIds.size.toString()
                val mentionModels = post.mentionIds.map { userId ->
                    AvatarModel(
                        userId = userId,
                        avatarUrl = post.users[userId.toString()]?.avatar
                    )
                }
                avatarMentionedStack.renderAvatarStack(
                    models = mentionModels,
                    moreButton = moreMentioned,
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
            } else mentionedContainer.isVisible = false
        }
    }

    private fun error(error: String) {
        val messageResId = when (error) {
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