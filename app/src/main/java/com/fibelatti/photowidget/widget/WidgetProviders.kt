package com.fibelatti.photowidget.widget

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Job

/**
 * Entry point to the app's widget providers. Callers can query and render widgets without
 * depending directly on a [Context].
 */
interface WidgetProviders {

    /** IDs of the widgets currently placed by [PhotoWidgetProvider]. */
    fun photoWidgetIds(): List<Int>

    /** IDs of the widgets currently placed by [TransparentWidgetProvider]. */
    fun transparentWidgetIds(): List<Int>

    /** See [PhotoWidgetProvider.update]. */
    fun updatePhotoWidget(appWidgetId: Int, allowCrossfade: Boolean = false): Job

    /** See [TransparentWidgetProvider.update]. */
    fun updateTransparentWidget(appWidgetId: Int)
}

class DefaultWidgetProviders @Inject constructor(
    @ApplicationContext private val context: Context,
) : WidgetProviders {

    override fun photoWidgetIds(): List<Int> = PhotoWidgetProvider.ids(context = context)

    override fun transparentWidgetIds(): List<Int> = TransparentWidgetProvider.ids(context = context)

    override fun updatePhotoWidget(appWidgetId: Int, allowCrossfade: Boolean): Job = PhotoWidgetProvider.update(
        context = context,
        appWidgetId = appWidgetId,
        allowCrossfade = allowCrossfade,
    )

    override fun updateTransparentWidget(appWidgetId: Int) {
        TransparentWidgetProvider.update(context = context, appWidgetId = appWidgetId)
    }
}
