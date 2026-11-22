package ru.netology.nework.util

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import ru.netology.nework.R

enum class FragmentMode(
    @param:StringRes val titleRes: Int,
    @param:DrawableRes val iconRes: Int? = null,
    val isActionCheckable: Boolean = false,
    val hideIfEmpty: Boolean = true
) {
    LIKERS(
        titleRes = R.string.likers,
        iconRes = R.drawable.ic_like_24,
        isActionCheckable = true,
        hideIfEmpty = false
    ),
    PARTICIPANTS(
        titleRes = R.string.participants,
        iconRes = R.drawable.ic_people_24,
        isActionCheckable = true,
        hideIfEmpty = false
    ),
    MENTIONED(
        titleRes = R.string.mentioned,
        iconRes = R.drawable.ic_people_24,
        isActionCheckable = false,
        hideIfEmpty = true
    ),
    SPEAKERS(
        titleRes = R.string.speakers,
        iconRes = null,
        isActionCheckable = false,
        hideIfEmpty = true
    ),
    SELECTED(
        titleRes = R.string.select_users
    ),
    USERS(
        titleRes = R.string.app_name
    )
}