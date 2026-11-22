package com.fibelatti.photowidget.platform

import android.content.ActivityNotFoundException
import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import com.fibelatti.photowidget.R
import timber.log.Timber

/**
 * Delegates to [ActivityResultLauncher.launch], informing the user with a dialog when no installed
 * app can handle the request, such as when the system file picker is missing or disabled.
 *
 * @return true if the request was launched, false otherwise.
 */
fun <I> ActivityResultLauncher<I>.launchOrNotify(context: Context, input: I): Boolean {
    return try {
        launch(input)
        true
    } catch (e: ActivityNotFoundException) {
        Timber.e(e, "No activity found to handle the request.")
        context.showMaterialAlertDialog {
            setMessage(R.string.error_no_app_to_handle_action)
            setPositiveButton(R.string.photo_widget_action_got_it) { _, _ -> }
        }
        false
    }
}
