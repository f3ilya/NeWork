package ru.netology.nework.extensions

import android.widget.ImageView
import com.bumptech.glide.Glide
import ru.netology.nework.R

fun ImageView.load(url: String?, circle: Boolean = false) {
    Glide.with(this)
        .load(url)
        .placeholder(R.drawable.ic_loading_48dp)
        .error(R.drawable.ic_error_48dp)
        .let { if (circle) it.circleCrop() else it}
        .timeout(10_000)
        .into(this)
}