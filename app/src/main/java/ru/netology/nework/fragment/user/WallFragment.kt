package ru.netology.nework.fragment.user

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import dev.androidbroadcast.vbpd.viewBinding
import ru.netology.nework.R
import ru.netology.nework.databinding.FragmentPostsBinding
import ru.netology.nework.media.MediaPlaybackManager
import javax.inject.Inject

@AndroidEntryPoint
class WallFragment : Fragment(R.layout.fragment_posts) {
    companion object {
        private const val ARG_USER_ID = "userId"
        fun newInstance(userId: Long) = WallFragment().apply {
            arguments = Bundle().apply { putLong(ARG_USER_ID, userId) }
        }
    }

    private val binding by viewBinding(FragmentPostsBinding::bind)

    @Inject
    lateinit var playbackManager: MediaPlaybackManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }

}