package ru.netology.nework.fragment.map

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import dev.androidbroadcast.vbpd.viewBinding
import ru.netology.nework.R
import ru.netology.nework.databinding.FragmentMapBinding
import ru.netology.nework.extensions.checkAndRequestLocationPermission
import ru.netology.nework.extensions.registerLocationPermissionLauncher
import ru.netology.nework.map.YandexMapManager
import ru.netology.nework.util.OnSaveActionListener
import ru.netology.nework.viewmodel.map.MapViewModel
import javax.inject.Inject

@AndroidEntryPoint
class MapFragment : Fragment(R.layout.fragment_map), OnSaveActionListener {
    private val binding by viewBinding(FragmentMapBinding::bind)
    private val viewModel: MapViewModel by viewModels()

    @Inject
    lateinit var yandexMapManager: YandexMapManager
    private val locationPermissionLauncher = registerLocationPermissionLauncher {
        yandexMapManager.enableUserLocation()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupYandexMap()
        setupListeners()
    }

    private fun setupYandexMap() {
        yandexMapManager.bindMapView(binding.map)
        viewLifecycleOwner.lifecycle.addObserver(yandexMapManager)
        checkAndRequestLocationPermission(locationPermissionLauncher) {
            yandexMapManager.enableUserLocation()
        }
        yandexMapManager.setupLocationPicker { lat, long ->
            viewModel.changeMap(lat = lat, long = long)
        }
    }

    private fun setupListeners() {
        with(binding) {
            btnZoomIn.setOnClickListener {
                yandexMapManager.changeZoom(1F)
            }

            btnZoomOut.setOnClickListener {
                yandexMapManager.changeZoom(-1F)
            }

            btnShowMyLocation.setOnClickListener {
                checkAndRequestLocationPermission(locationPermissionLauncher) {
                    yandexMapManager.enableUserLocation()
                    yandexMapManager.moveToUserLocation()
                }
            }
        }
    }

    override fun onSaveAction() {
        val navController = findNavController()
        val prevFragment = navController.previousBackStackEntry ?: return
        prevFragment.savedStateHandle.let { handle ->
            handle["lat"] = viewModel.map.value.lat
            handle["long"] = viewModel.map.value.long
        }
        navController.popBackStack()
    }
}