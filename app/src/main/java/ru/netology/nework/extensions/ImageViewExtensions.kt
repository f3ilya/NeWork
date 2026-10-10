package ru.netology.nework.extensions

import android.widget.ImageView
import com.bumptech.glide.Glide
import ru.netology.nework.R

fun ImageView.load(url: String?, circle: Boolean = false) {
    if (circle && url.isNullOrEmpty()) {
        setImageResource(R.drawable.ic_avatar_placeholder_48)
        return
    }
    Glide.with(this)
        .load(url)
        .placeholder(R.drawable.ic_loading_48dp)
        .error(R.drawable.ic_error_48dp)
        .let { if (circle) it.circleCrop() else it}
        .timeout(10_000)
        .into(this)
}