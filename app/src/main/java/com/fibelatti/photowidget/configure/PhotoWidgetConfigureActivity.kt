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
import androidx.activity.OnBackPressedCallback
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
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetLoopingInterval
import com.fibelatti.photowidget.model.PhotoWidgetTapAction
import com.fibelatti.photowidget.platform.AppTheme
import com.fibelatti.photowidget.platform.SelectionDialog
import com.fibelatti.photowidget.platform.getAttributeColor
import com.fibelatti.photowidget.widget.PhotoWidgetProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.yalantis.ucrop.UCrop
import dagger.hilt.android.AndroidEntryPoint
import java.lang.ref.WeakReference

@AndroidEntryPoint
class PhotoWidgetConfigureActivity : AppCompatActivity() {

    private val viewModel by viewModels<PhotoWidgetConfigureViewModel>()

    private val photoPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents(),
        ::onPhotoPicked,
    )

    private val photoCropLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        ::onPhotoCropped,
    )

    private val photoDirPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
        ::onDirPicked,
    )

    private val onBackPressedCallback = object : OnBackPressedCallback(enabled = false) {

        override fun handleOnBackPressed() {
            MaterialAlertDialogBuilder(this@PhotoWidgetConfigureActivity)
                .setMessage(R.string.photo_widget_configure_navigate_back_warning)
                .setPositiveButton(R.string.photo_widget_action_yes) { _, _ ->
                    finish()
                }
                .setNegativeButton(R.string.photo_widget_action_no) { _, _ -> }
                .show()
        }
    }

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

                onBackPressedCallback.isEnabled = state.photoWidget.photos.isNotEmpty()

                PhotoWidgetConfigureScreen(
                    photoWidget = state.photoWidget,
                    selectedPhoto = state.selectedPhoto,
                    isProcessing = state.isProcessing,
                    onAspectRatioClick = ::showAspectRatioPicker,
                    onCropClick = viewModel::requestCrop,
                    onRemoveClick = viewModel::photoRemoved,
                    onMoveLeftClick = viewModel::moveLeft,
                    onMoveRightClick = viewModel::moveRight,
                    onChangeSource = viewModel::changeSource,
                    onShuffleClick = viewModel::toggleShuffle,
                    onPhotoPickerClick = ::launchPhotoPicker,
                    onDirPickerClick = ::launchFolderPicker,
                    onPhotoClick = viewModel::previewPhoto,
                    onLoopingIntervalPickerClick = ::showIntervalPicker,
                    onTapActionPickerClick = ::showTapActionPicker,
                    onShapeClick = viewModel::shapeSelected,
                    onCornerRadiusChange = viewModel::cornerRadiusSelected,
                    onAddToHomeClick = viewModel::addNewWidget,
                )

                LaunchedEffect(state.messages) {
                    state.messages.firstOrNull()?.let { handleMessage(it) }
                }
            }
        }

        onBackPressedDispatcher.addCallback(onBackPressedCallback)

        LocalBroadcastManager.getInstance(this).registerReceiver(
            finishReceiver,
            IntentFilter(ACTION_FINISH),
        )

        checkIntent()
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(finishReceiver)
        super.onDestroy()
    }

    private fun checkIntent() {
        intent.sharedPhotos?.let(viewModel::photoPicked)
    }

    private suspend fun handleMessage(message: PhotoWidgetConfigureState.Message) {
        when (message) {
            is PhotoWidgetConfigureState.Message.ImportFailed -> {
                MaterialAlertDialogBuilder(this)
                    .setMessage(R.string.photo_widget_configure_import_error)
                    .setPositiveButton(R.string.photo_widget_action_continue) { _, _ -> }
                    .setOnDismissListener { viewModel.messageHandled(message = message) }
                    .show()
            }

            is PhotoWidgetConfigureState.Message.LaunchCrop -> {
                launchPhotoCrop(
                    sourceUri = message.source,
                    destinationUri = message.destination,
                    aspectRatio = message.aspectRatio,
                )
                viewModel.messageHandled(message = message)
            }

            is PhotoWidgetConfigureState.Message.RequestPin -> {
                requestPin(photoWidget = message.photoWidget)
                viewModel.messageHandled(message = message)
            }

            is PhotoWidgetConfigureState.Message.AddWidget -> {
                addNewWidget(appWidgetId = message.appWidgetId)
                viewModel.messageHandled(message = message)
            }

            is PhotoWidgetConfigureState.Message.CancelWidget -> {
                finish()
                viewModel.messageHandled(message = message)
            }
        }
    }

    private fun showAspectRatioPicker() {
        SelectionDialog.show(
            context = this,
            title = getString(R.string.photo_widget_aspect_ratio_title),
            options = PhotoWidgetAspectRatio.entries,
            optionName = { option -> getString(option.label) },
            onOptionSelected = viewModel::setAspectRatio,
        )
    }

    private fun launchPhotoPicker() {
        photoPickerLauncher.launch("image/*")
    }

    private fun launchFolderPicker() {
        photoDirPickerLauncher.launch(null)
    }

    private fun onPhotoPicked(source: List<Uri>) {
        viewModel.photoPicked(source = source)
    }

    private fun onDirPicked(uri: Uri?) {
        viewModel.dirPicked(source = uri)
    }

    private fun launchPhotoCrop(
        sourceUri: Uri,
        destinationUri: Uri,
        aspectRatio: PhotoWidgetAspectRatio,
    ) {
        val intent = UCrop.of(sourceUri, destinationUri)
            .apply {
                if (PhotoWidgetAspectRatio.ORIGINAL != aspectRatio) {
                    withAspectRatio(aspectRatio.x, aspectRatio.y)
                }
            }
            .withMaxResultSize(PhotoWidget.MAX_DIMENSION, PhotoWidget.MAX_DIMENSION)
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
        result.data?.let(UCrop::getOutput)?.path
            ?.let(viewModel::photoCropped)
            ?: run { viewModel.cropCancelled() }
    }

    private fun showIntervalPicker(
        loopingInterval: PhotoWidgetLoopingInterval,
        intervalBasedLoopingEnabled: Boolean,
    ) {
        PhotoWidgetIntervalPicker.show(
            context = this,
            currentInterval = loopingInterval,
            currentIntervalBasedLoopingEnabled = intervalBasedLoopingEnabled,
            onApplyClick = viewModel::intervalSelected,
        )
    }

    private fun showTapActionPicker() {
        SelectionDialog.show(
            context = this,
            title = getString(R.string.photo_widget_configure_tap_action),
            options = PhotoWidgetTapAction.entries,
            optionName = { option -> getString(option.title) },
            onOptionSelected = viewModel::tapActionSelected,
        )
    }

    private fun addNewWidget(appWidgetId: Int) {
        PhotoWidgetProvider.update(
            context = this,
            appWidgetId = appWidgetId,
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

    private suspend fun requestPin(photoWidget: PhotoWidget) {
        val remoteViews = PhotoWidgetProvider.createRemoteViews(
            context = this,
            photoWidget = photoWidget,
        )
        val previewBundle = bundleOf(
            AppWidgetManager.EXTRA_APPWIDGET_PREVIEW to remoteViews,
        )

        val callbackIntent = Intent(this, PhotoWidgetPinnedReceiver::class.java)
            .apply { this.photoWidget = photoWidget }
        val successCallback = PendingIntent.getBroadcast(
            /* context = */ this,
            /* requestCode = */ 0,
            /* intent = */ callbackIntent,
            /* flags = */ PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        PhotoWidgetPinnedReceiver.preview = WeakReference(remoteViews)
        PhotoWidgetPinnedReceiver.callbackIntent = WeakReference(callbackIntent)

        AppWidgetManager.getInstance(this).requestPinAppWidget(
            /* provider = */ ComponentName(this, PhotoWidgetProvider::class.java),
            /* extras = */ previewBundle,
            /* successCallback = */ successCallback,
        )
    }

    companion object {

        const val ACTION_FINISH = "FINISH_PHOTO_WIDGET_CONFIGURE_ACTIVITY"
    }
}
