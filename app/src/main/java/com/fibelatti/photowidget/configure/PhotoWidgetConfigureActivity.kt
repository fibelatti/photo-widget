package com.fibelatti.photowidget.configure

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.os.bundleOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetLoopingInterval
import com.fibelatti.photowidget.platform.AppTheme
import com.fibelatti.photowidget.platform.SelectionDialog
import com.fibelatti.photowidget.platform.getAttributeColor
import com.fibelatti.photowidget.widget.PhotoWidgetProvider
import com.fibelatti.ui.foundation.toStableList
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.yalantis.ucrop.UCrop
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PhotoWidgetConfigureActivity : AppCompatActivity() {

    private val viewModel by viewModels<PhotoWidgetConfigureViewModel>()

    private val photoPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent(),
        ::onPhotoPicked,
    )

    private val photoCropLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        ::onPhotoCropped,
    )

    private val finishReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_FINISH) {
                widgetAdded(appWidgetId = intent.appWidgetId)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Set the result to CANCELED. This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(RESULT_CANCELED)

        setContent {
            AppTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()

                PhotoWidgetConfigureScreen(
                    photos = state.photos.toStableList(),
                    onPhotoPickerClick = ::launchPhotoPicker,
                    onPhotoLongClick = ::showRemovePhotoDialog,
                    loopingInterval = state.loopingInterval,
                    onLoopingIntervalPickerClick = ::showIntervalPicker,
                    aspectRatio = state.aspectRatio,
                    shapeId = state.shapeId,
                    onShapeClick = viewModel::shapeSelected,
                    onAddToHomeClick = viewModel::addNewWidget,
                )

                LaunchedEffect(state.message) {
                    handleMessage(state.message)
                }
            }
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(
            finishReceiver,
            IntentFilter(ACTION_FINISH),
        )
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(finishReceiver)
        super.onDestroy()
    }

    private fun handleMessage(message: PhotoWidgetConfigureState.Message?) {
        when (message) {
            is PhotoWidgetConfigureState.Message.LaunchCrop -> {
                launchPhotoCrop(
                    sourceUri = message.source,
                    destinationUri = message.destination,
                    aspectRatio = message.aspectRatio,
                )
            }

            is PhotoWidgetConfigureState.Message.RequestPin -> {
                requestPin(
                    photoPath = message.photoPath,
                    enableLooping = message.enableLooping,
                    loopingInterval = message.loopingInterval,
                    aspectRatio = message.aspectRatio,
                    shapeId = message.shapeId,
                )
            }

            is PhotoWidgetConfigureState.Message.AddWidget -> {
                addNewWidget(
                    appWidgetId = message.appWidgetId,
                    photoPath = message.photoPath,
                    aspectRatio = message.aspectRatio,
                    shapeId = message.shapeId,
                )
            }

            is PhotoWidgetConfigureState.Message.CancelWidget -> {
                finish()
            }

            null -> return
        }

        viewModel.messageHandled()
    }

    private fun launchPhotoPicker() {
        photoPickerLauncher.launch("image/*")
    }

    private fun onPhotoPicked(uri: Uri?) {
        viewModel.photoSelected(uri)
    }

    private fun launchPhotoCrop(
        sourceUri: Uri,
        destinationUri: Uri,
        aspectRatio: PhotoWidgetAspectRatio,
    ) {
        val intent = UCrop.of(sourceUri, destinationUri)
            .withAspectRatio(aspectRatio.x, aspectRatio.y)
            .withMaxResultSize(1_000, 1_000)
            .withOptions(
                UCrop.Options().apply {
                    setCompressionFormat(Bitmap.CompressFormat.PNG)
                    setActiveControlsWidgetColor(getAttributeColor(android.R.attr.colorPrimary))
                },
            )
            .getIntent(this)

        photoCropLauncher.launch(intent)
    }

    private fun onPhotoCropped(result: ActivityResult) {
        result.data?.let(UCrop::getOutput)?.path?.let(viewModel::photoCropped)
    }

    private fun showRemovePhotoDialog(photoPath: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.photo_widget_configure_delete_photo_title)
            .setPositiveButton(R.string.photo_widget_configure_delete_photo_yes) { _, _ ->
                viewModel.photoRemoved(photoPath)
            }
            .setNegativeButton(R.string.photo_widget_configure_delete_photo_no) { _, _ -> }
            .show()
    }

    private fun showIntervalPicker() {
        SelectionDialog.show(
            context = this,
            title = getString(R.string.photo_widget_configure_select_interval),
            options = PhotoWidgetLoopingInterval.entries.toStableList(),
            optionName = { option -> getString(option.title) },
            onOptionSelected = viewModel::intervalSelected,
        )
    }

    private fun addNewWidget(
        appWidgetId: Int,
        photoPath: String,
        aspectRatio: PhotoWidgetAspectRatio,
        shapeId: String,
    ) {
        PhotoWidgetProvider.update(
            context = this,
            appWidgetId = appWidgetId,
            photoPath = photoPath,
            aspectRatio = aspectRatio,
            shapeId = shapeId,
        )

        widgetAdded(appWidgetId = appWidgetId)
    }

    private fun widgetAdded(appWidgetId: Int) {
        val resultValue = Intent().apply {
            this.appWidgetId = appWidgetId
        }

        setResult(RESULT_OK, resultValue)

        finish()
    }

    private fun requestPin(
        photoPath: String,
        enableLooping: Boolean,
        loopingInterval: PhotoWidgetLoopingInterval,
        aspectRatio: PhotoWidgetAspectRatio,
        shapeId: String,
    ) {
        val remoteViews = PhotoWidgetProvider.createRemoteViews(
            context = this,
            photoPath = photoPath,
            aspectRatio = aspectRatio,
            shapeId = shapeId,
        )
        val previewBundle = bundleOf(
            AppWidgetManager.EXTRA_APPWIDGET_PREVIEW to remoteViews,
        )

        val callbackIntent = Intent(this, PhotoWidgetPinnedReceiver::class.java).apply {
            this.enableLooping = enableLooping
            this.loopingInterval = loopingInterval
            this.aspectRatio = aspectRatio
            this.shapeId = shapeId
        }

        val successCallback = PendingIntent.getBroadcast(
            this,
            0,
            callbackIntent,
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        AppWidgetManager.getInstance(this).requestPinAppWidget(
            ComponentName(this, PhotoWidgetProvider::class.java),
            previewBundle,
            successCallback,
        )
    }

    companion object {

        const val ACTION_FINISH = "FINISH_PHOTO_WIDGET_CONFIGURE_ACTIVITY"
    }
}
