package ru.netology.nework.extensions

import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.Target
import ru.netology.nework.R

fun ImageView.load(url: String?, circle: Boolean = false, autoSize: Boolean = false) {
    if (url.isNullOrEmpty()) {
        setImageResource(R.drawable.ic_avatar_placeholder_48)
        return
    }
    Glide.with(this)
        .load(url)
        .placeholder(R.drawable.ic_loading_48dp)
        .error(R.drawable.ic_error_48dp)
        .timeout(10_000)
        .let {
            when {
                circle -> it.circleCrop()
                autoSize -> {
                    it.override(Target.SIZE_ORIGINAL)
                    it.dontTransform()
                }

                else -> it.fitCenter()
            }
        }
        .into(this)
}