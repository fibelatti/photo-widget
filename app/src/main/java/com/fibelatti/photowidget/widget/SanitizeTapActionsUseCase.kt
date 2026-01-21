package com.fibelatti.photowidget.widget

import android.content.Context
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetTapAction
import com.fibelatti.photowidget.model.TapActionArea
import com.fibelatti.photowidget.platform.getInstalledApp
import com.fibelatti.photowidget.widget.data.PhotoWidgetStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SanitizeTapActionsUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val photoWidgetStorage: PhotoWidgetStorage,
) {

    operator fun invoke(appWidgetId: Int, photoWidget: PhotoWidget): PhotoWidget {
        return photoWidget.copy(
            tapActions = photoWidget.tapActions.copy(
                left = sanitize(
                    appWidgetId = appWidgetId,
                    tapAction = photoWidget.tapActions.left,
                    tapActionArea = TapActionArea.LEFT,
                ),
                center = sanitize(
                    appWidgetId = appWidgetId,
                    tapAction = photoWidget.tapActions.center,
                    tapActionArea = TapActionArea.CENTER,
                ),
                right = sanitize(
                    appWidgetId = appWidgetId,
                    tapAction = photoWidget.tapActions.right,
                    tapActionArea = TapActionArea.RIGHT,
                ),
            ),
        )
    }

    private fun sanitize(
        appWidgetId: Int,
        tapAction: PhotoWidgetTapAction,
        tapActionArea: TapActionArea,
    ): PhotoWidgetTapAction {
        return when (tapAction) {
            is PhotoWidgetTapAction.ViewInGallery if context.getInstalledApp(tapAction.galleryApp) == null -> {
                tapAction.copy(galleryApp = null)
            }

            is PhotoWidgetTapAction.AppShortcut if context.getInstalledApp(tapAction.appShortcut) == null -> {
                tapAction.copy(appShortcut = null)
            }

            is PhotoWidgetTapAction.AppFolder if tapAction.shortcuts.isNotEmpty() -> {
                tapAction.copy(shortcuts = tapAction.shortcuts.filter { context.getInstalledApp(it) != null })
            }

            else -> tapAction
        }.also { sanitized: PhotoWidgetTapAction ->
            photoWidgetStorage.saveWidgetTapAction(
                appWidgetId = appWidgetId,
                tapAction = sanitized,
                tapActionArea = tapActionArea,
            )
        }
    }
}
