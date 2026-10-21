package ru.netology.nework.extensions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.view.View
import androidx.fragment.app.Fragment
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import com.github.dhaval2404.imagepicker.ImagePicker
import com.github.dhaval2404.imagepicker.constant.ImageProvider
import com.google.android.material.snackbar.Snackbar
import ru.netology.nework.R
import java.io.File

fun Fragment.hideKeyboard() {
    val view = activity?.currentFocus ?: View(activity)
    val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    imm?.hideSoftInputFromWindow(view.windowToken, 0)
}

fun Fragment.registerLocationPermissionLauncher(
    onGranted: ()  -> Unit,
) : ActivityResultLauncher<String> {
    return registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted){
            onGranted()
        } else {
            if (!shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                showSettingsSnackbar()
            } else {
                Snackbar.make(
                    requireView(),
                    getString(R.string.access_to_geolocation_is_denied),
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }
}

fun Fragment.showSettingsSnackbar() {
    Snackbar.make(
        requireView(),
        getString(R.string.geolocation_is_disabled),
        Snackbar.LENGTH_INDEFINITE
    ).setAction(getString(R.string.settings)) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", requireContext().packageName, null)
        }
        startActivity(intent)
    }.show()
}

fun Fragment.checkAndRequestLocationPermission(
    launcher: ActivityResultLauncher<String>,
    onGranted: () -> Unit,
) {
    when {
        ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED -> onGranted()

        shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
            requireContext().showConfirmationDialog(
                title = getString(R.string.access_to_geolocation),
                message = getString(R.string.the_permission_is_needed_to),
                positive = getString(R.string.provide),
                onConfirm = { launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
            )
        }

        else -> launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }
}

fun Fragment.registerImagePickerLauncher(
    onSuccess: (uri: Uri, file: File) -> Unit,
    onError: (throwable: Throwable) -> Unit,
): ActivityResultLauncher<Intent> {
    return registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        when (result.resultCode) {
            ImagePicker.RESULT_ERROR -> {
                val errorMessage = ImagePicker.getError(result.data)
                onError(Exception(errorMessage))
            }
            Activity.RESULT_OK -> {
                val uri: Uri? = result.data?.data
                val file = uri?.toFile()
                if (uri != null && file != null) onSuccess(uri, file)
            }
        }
    }
}

fun Fragment.showPhotoSelectionDialog(
    launcher: ActivityResultLauncher<Intent>,
    isPhotoAttached: Boolean = false,
    onRemovePhoto: () -> Unit = {},
    title: Int = R.string.choose_a_picture
) {
    requireContext().showConfirmationDialog(
        title = getString(title),
        positive = getString(R.string.description_select_photo),
        negative = getString(R.string.description_take_photo),
        neutral = getString(R.string.remove),
        isNeed = isPhotoAttached,
        onConfirm = {
            ImagePicker.with(this)
                .crop()
                .compress(2048)
                .provider(ImageProvider.GALLERY)
                .galleryMimeTypes(arrayOf("image/png", "image/jpeg"))
                .createIntent { intent -> launcher.launch(intent) }
        },
        onCancel = {
            ImagePicker.with(this)
                .crop()
                .compress(2048)
                .provider(ImageProvider.CAMERA)
                .createIntent { intent -> launcher.launch(intent) }
        },
        onNeutral = {
            onRemovePhoto()
        }
    )
}

fun Fragment.authorizationIsRequired(
    onNavigate: () -> Unit,
) {
    requireContext().showConfirmationDialog(
        title = getString(R.string.authorization_is_required_for_this_action),
        message = getString(R.string.authorization_is_required_message),
        onConfirm = onNavigate,
        positive = getString(R.string.log_in)
    )
}