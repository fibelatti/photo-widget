package com.fibelatti.photowidget.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import com.fibelatti.photowidget.chooser.PhotoWidgetChooserActivity
import com.fibelatti.photowidget.configure.appWidgetId
import com.fibelatti.photowidget.folder.PhotoWidgetAppFolderActivity
import com.fibelatti.photowidget.model.PhotoWidgetTapAction
import com.fibelatti.photowidget.platform.getInstalledApp
import com.fibelatti.photowidget.platform.getLaunchIntent
import com.fibelatti.photowidget.platform.setIdentifierCompat
import com.fibelatti.photowidget.platform.sharePhotoChooserIntent
import com.fibelatti.photowidget.viewer.PhotoWidgetViewerActivity
import timber.log.Timber

object TapActionPendingIntentFactory {

    fun create(
        context: Context,
        appWidgetId: Int,
        tapAction: PhotoWidgetTapAction,
        isLocked: Boolean,
        shouldDisableTap: Boolean,
        originalPhotoPath: String?,
        externalUri: Uri?,
    ): PendingIntent? {
        Timber.d(
            "Determining intent (" +
                "appWidgetId=$appWidgetId," +
                "tapAction=$tapAction," +
                "isLocked=$isLocked," +
                "shouldDisableTap=$shouldDisableTap," +
                "originalPhotoPath=$originalPhotoPath," +
                "externalUri=$externalUri" +
                ")",
        )

        val shouldIgnoreAction: Boolean = when {
            shouldDisableTap && tapAction !is PhotoWidgetTapAction.ToggleCycling -> true
            tapAction.isPhotoChangingAction -> isLocked
            else -> false
        }

        if (shouldIgnoreAction) {
            Timber.d("Ignoring action (isPhotoChangingAction=${tapAction.isPhotoChangingAction})")
            return null
        }

        when (tapAction) {
            is PhotoWidgetTapAction.None -> return null

            is PhotoWidgetTapAction.ViewFullScreen -> {
                val clickIntent = Intent(context, PhotoWidgetViewerActivity::class.java).apply {
                    setIdentifierCompat("$appWidgetId")
                    this.appWidgetId = appWidgetId
                }
                return PendingIntent.getActivity(
                    /* context = */ context,
                    /* requestCode = */ appWidgetId,
                    /* intent = */ clickIntent,
                    /* flags = */ PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            }

            is PhotoWidgetTapAction.ViewInGallery -> {
                if (externalUri == null) return null

                val intent = Intent(Intent.ACTION_VIEW)
                    .setDataAndType(externalUri, "image/*")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .setIdentifierCompat("$appWidgetId")

                if (context.getInstalledApp(packageName = tapAction.galleryApp) != null) {
                    intent.setPackage(tapAction.galleryApp)
                }

                return PendingIntent.getActivity(
                    /* context = */ context,
                    /* requestCode = */ appWidgetId,
                    /* intent = */ intent,
                    /* flags = */ PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            }

            is PhotoWidgetTapAction.ViewNextPhoto -> {
                return getChangePhotoPendingIntent(
                    context = context,
                    appWidgetId = appWidgetId,
                    action = PhotoWidgetProvider.Action.VIEW_NEXT_PHOTO,
                )
            }

            is PhotoWidgetTapAction.ViewPreviousPhoto -> {
                return getChangePhotoPendingIntent(
                    context = context,
                    appWidgetId = appWidgetId,
                    action = PhotoWidgetProvider.Action.VIEW_PREVIOUS_PHOTO,
                )
            }

            is PhotoWidgetTapAction.ChooseNextPhoto -> {
                val clickIntent = Intent(context, PhotoWidgetChooserActivity::class.java).apply {
                    setIdentifierCompat("$appWidgetId")
                    this.appWidgetId = appWidgetId
                }
                return PendingIntent.getActivity(
                    /* context = */ context,
                    /* requestCode = */ appWidgetId,
                    /* intent = */ clickIntent,
                    /* flags = */ PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            }

            is PhotoWidgetTapAction.ToggleCycling -> {
                val intent = Intent(context, ToggleCyclingFeedbackActivity::class.java).apply {
                    setIdentifierCompat("$appWidgetId")
                    this.appWidgetId = appWidgetId
                }
                return PendingIntent.getActivity(
                    /* context = */ context,
                    /* requestCode = */ appWidgetId,
                    /* intent = */ intent,
                    /* flags = */ PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            }

            is PhotoWidgetTapAction.AppShortcut -> {
                if (tapAction.appShortcut == null) return null

                val launchIntent: Intent = context.getLaunchIntent(packageName = tapAction.appShortcut)
                    ?: return null

                launchIntent.setIdentifierCompat("$appWidgetId")

                return PendingIntent.getActivity(
                    /* context = */ context,
                    /* requestCode = */ appWidgetId,
                    /* intent = */ launchIntent,
                    /* flags = */ PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            }

            is PhotoWidgetTapAction.AppFolder -> {
                val intent: Intent = PhotoWidgetAppFolderActivity.newIntent(
                    context = context,
                    appWidgetId = appWidgetId,
                    appFolderTapAction = tapAction,
                )
                return PendingIntent.getActivity(
                    /* context = */ context,
                    /* requestCode = */ appWidgetId + tapAction.hashCode(),
                    /* intent = */ intent,
                    /* flags = */ PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            }

            is PhotoWidgetTapAction.UrlShortcut -> {
                if (tapAction.url.isNullOrBlank()) return null

                val intent = Intent(Intent.ACTION_VIEW, tapAction.url.toUri())
                    .setIdentifierCompat("$appWidgetId")

                return PendingIntent.getActivity(
                    /* context = */ context,
                    /* requestCode = */ appWidgetId,
                    /* intent = */ intent,
                    /* flags = */ PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            }

            is PhotoWidgetTapAction.SharePhoto -> {
                val intent: Intent = sharePhotoChooserIntent(
                    context = context,
                    originalPhotoPath = originalPhotoPath,
                    externalUri = externalUri,
                ) ?: return null

                return PendingIntent.getActivity(
                    /* context = */ context,
                    /* requestCode = */ appWidgetId,
                    /* intent = */ intent,
                    /* flags = */ PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            }

            is PhotoWidgetTapAction.RemovePhoto -> {
                return getChangePhotoPendingIntent(
                    context = context,
                    appWidgetId = appWidgetId,
                    action = PhotoWidgetProvider.Action.REMOVE_PHOTO,
                )
            }
        }
    }

    fun getChangePhotoPendingIntent(
        context: Context,
        appWidgetId: Int,
        action: PhotoWidgetProvider.Action = PhotoWidgetProvider.Action.VIEW_NEXT_PHOTO,
    ): PendingIntent {
        val intent = Intent(context, PhotoWidgetProvider::class.java).apply {
            setIdentifierCompat("$appWidgetId")
            this.appWidgetId = appWidgetId
            this.action = action.value
        }
        return PendingIntent.getBroadcast(
            /* context = */ context,
            /* requestCode = */ appWidgetId,
            /* intent = */ intent,
            /* flags = */ PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
