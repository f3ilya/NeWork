package ru.netology.nework.extensions

import android.util.TypedValue
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.ShapeAppearanceModel
import ru.netology.nework.model.AvatarModel

fun LinearLayout.renderAvatarStack(
    models: List<AvatarModel>,
    moreButton: MaterialButton,
    maxToShow: Int = 5,
    onAvatarClickListener: (Long) -> Unit,
    onMoreClickListener: () -> Unit,
) {
    this.removeAllViews()

    if (models.isEmpty()) {
        moreButton.isVisible = false
        return
    }

    val density = resources.displayMetrics.density
    val sizeInPx = (48 * density).toInt()
    val negativeMarginInPx = (-8 * density).toInt()

    models.take(maxToShow).forEachIndexed { index, model ->
        val imageView = ShapeableImageView(context).apply {
            shapeAppearanceModel = ShapeAppearanceModel.Builder()
                .setAllCornerSizes { sizeInPx / 2f }
                .build()

            val typedValue = TypedValue()
            context.theme.resolveAttribute(
                com.google.android.material.R.attr.colorSurface,
                typedValue,
                true
            )
            strokeColor = ContextCompat.getColorStateList(context, typedValue.resourceId)
            strokeWidth = 2 * density
            setPadding(
                density.toInt(),
                density.toInt(),
                density.toInt(),
                density.toInt(),
            )
            isClickable = true
            isFocusable = true
            setOnClickListener { onAvatarClickListener(model.userId) }
        }

        val params = LinearLayout.LayoutParams(sizeInPx, sizeInPx).apply {
            if (index > 0) leftMargin = negativeMarginInPx
        }
        imageView.layoutParams = params
        imageView.load(model.avatarUrl, true)
        this.addView(imageView)
    }

    if (models.size > maxToShow) {
        moreButton.isVisible = true
        moreButton.setOnClickListener { onMoreClickListener() }
    } else {
        moreButton.isVisible = false
    }
}