package ru.netology.nework.viewmodel.map

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import ru.netology.nework.model.MapModel
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor() : ViewModel() {
    private val _map = MutableStateFlow(MapModel())
    val map: StateFlow<MapModel> = _map.asStateFlow()

    fun changeMap(lat: Double, long: Double) {
        _map.update { model ->
            model.copy(
                lat = lat,
                long = long
            )
        }
    }
}