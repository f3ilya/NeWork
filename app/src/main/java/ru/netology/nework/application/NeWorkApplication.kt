package ru.netology.nework.application

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import ru.netology.nework.BuildConfig


@HiltAndroidApp
class NeWorkApplication : Application() {
    override fun onCreate() {
        super.onCreate()
//        MapKitFactory.setApiKey(BuildConfig.MAPS_API_KEY)
    }
}