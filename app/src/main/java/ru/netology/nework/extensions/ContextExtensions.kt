package ru.netology.nework.extensions

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import okio.FileNotFoundException
import ru.netology.nework.R
import java.io.File
import java.io.FileOutputStream

fun Context.showConfirmationDialog(
    title: String,
    message: String? = null,
    onConfirm: () -> Unit,
    positive: String = getString(R.string.ok),
    negative: String = getString(R.string.cancel),
    neutral: String = "",
    onNeutral: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null,
    isNeed: Boolean = false,
) {
    MaterialAlertDialogBuilder(this)
        .setTitle(title)
        .setMessage(message)
        .setNegativeButton(negative) { dialog, _ ->
            onCancel?.invoke()
            dialog.dismiss()
        }
        .setPositiveButton(positive) { dialog, _ ->
            onConfirm()
            dialog.dismiss()
        }
        .setOnCancelListener { dialog ->
            dialog.dismiss()
        }
        .apply { if (isNeed) this.setNeutralButton(neutral) { dialog, _ ->
            onNeutral?.invoke()
            dialog.dismiss()
        } }
        .show()
}

fun Context.shareIntent(content: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, content)
    }
    val shareIntent = Intent.createChooser(
        intent,
        getString(R.string.chooser_share_post)
    )
    startActivity(shareIntent)
}

fun Context.getBitmapFromVectorDrawable(
    @DrawableRes drawableId: Int,
    @ColorInt tintColor: Int? = null,
): Bitmap? {
    val drawable = ContextCompat.getDrawable(this, drawableId) ?: return null

    tintColor?.also(drawable::setTint)

    val bitmap = createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)

    return bitmap
}

fun Context.uriToFile(uri: Uri): File {
    val inputStream = contentResolver.openInputStream(uri) ?: throw FileNotFoundException()
    val tempFile = File(cacheDir, "upload_${System.currentTimeMillis()}.tmp")
    FileOutputStream(tempFile).use { outputStream ->
        inputStream.use { inputStream ->
            inputStream.copyTo(outputStream)
        }
    }
    return tempFile
}