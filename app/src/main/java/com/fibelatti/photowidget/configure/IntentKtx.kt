package com.fibelatti.photowidget.configure

import android.appwidget.AppWidgetManager
import android.content.Intent
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetLoopingInterval
import com.fibelatti.photowidget.platform.intentExtras

var Intent.appWidgetId: Int by intentExtras(
    key = AppWidgetManager.EXTRA_APPWIDGET_ID,
    default = AppWidgetManager.INVALID_APPWIDGET_ID,
)

var Intent.order: List<String> by intentExtras()

var Intent.enableLooping: Boolean by intentExtras()

var Intent.loopingInterval: PhotoWidgetLoopingInterval by intentExtras()

var Intent.shapeId: String by intentExtras()

var Intent.aspectRatio: PhotoWidgetAspectRatio by intentExtras()
