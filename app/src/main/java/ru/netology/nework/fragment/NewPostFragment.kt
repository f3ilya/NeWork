package ru.netology.nework.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import dev.androidbroadcast.vbpd.viewBinding
import ru.netology.nework.R
import ru.netology.nework.databinding.FragmentNewPostBinding
import ru.netology.nework.extensions.checkAndRequestLocationPermission
import ru.netology.nework.extensions.registerLocationPermissionLauncher
import ru.netology.nework.map.YandexMapManager
import javax.inject.Inject

@AndroidEntryPoint
class NewPostFragment : Fragment(R.layout.fragment_new_post) {
    private val binding by viewBinding(FragmentNewPostBinding::bind)
    @Inject
    lateinit var yandexMapManager: YandexMapManager
    private val locationPermissionLauncher = registerLocationPermissionLauncher {
        yandexMapManager.enableUserLocation()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupYandexMap()
    }

    private fun setupYandexMap() {
        viewLifecycleOwner.lifecycle.addObserver(yandexMapManager)
        checkAndRequestLocationPermission(locationPermissionLauncher) {
            yandexMapManager.enableUserLocation()
        }
    }
}