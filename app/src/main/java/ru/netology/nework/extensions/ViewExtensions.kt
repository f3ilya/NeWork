package ru.netology.nework.extensions

import android.view.View
import androidx.appcompat.widget.PopupMenu
import ru.netology.nework.R

fun View.showOptionsMenu(
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    PopupMenu(context, this).apply {
        inflate(R.menu.menu_options)
        setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.edit -> {
                    onEdit()
                    true
                }

                R.id.remove -> {
                    onRemove()
                    true
                }

                else -> false
            }
        }
    }.show()
}