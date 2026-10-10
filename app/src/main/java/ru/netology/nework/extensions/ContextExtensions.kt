package ru.netology.nework.extensions

import android.content.Context
import android.content.Intent
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import ru.netology.nework.R

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
        .setOnCancelListener {
            onCancel?.invoke()
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