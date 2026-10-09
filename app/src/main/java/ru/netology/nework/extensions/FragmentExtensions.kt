package ru.netology.nework.extensions

import android.content.Context
import android.view.View
import androidx.fragment.app.Fragment
import android.view.inputmethod.InputMethodManager

fun Fragment.hideKeyboard() {
    val view = activity?.currentFocus ?: View(activity)
    val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    imm?.hideSoftInputFromWindow(view.windowToken, 0)
}