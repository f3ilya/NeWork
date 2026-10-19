package ru.netology.nework.util

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.ColorInt
import com.yandex.runtime.image.ImageProvider
import ru.netology.nework.extensions.getBitmapFromVectorDrawable

data class ImageInfo(
    @field:DrawableRes val id: Int,
    @field:ColorInt val tintColor: Int? = null
)

class DrawableImageProvider(
    private val context: Context,
    private val imageInfo: ImageInfo
) : ImageProvider() {
    override fun getImage() = context.getBitmapFromVectorDrawable(
        drawableId = imageInfo.id,
        tintColor = imageInfo.tintColor,
    )

    override fun getId() = imageInfo.toString()
}