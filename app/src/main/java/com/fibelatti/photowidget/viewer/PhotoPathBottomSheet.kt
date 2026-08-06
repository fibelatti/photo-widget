package com.fibelatti.photowidget.viewer

import android.content.ClipData
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.toClipEntry
import androidx.core.net.toUri
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.platform.LocalAppCompatActivity
import com.fibelatti.photowidget.platform.toFolderDocumentUri
import com.fibelatti.ui.component.AppSheetState
import com.fibelatti.ui.component.SelectionDialogBottomSheet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun PhotoPathBottomSheet(
    sheetState: AppSheetState,
) {
    val input: Pair<String, String> = sheetState.bottomSheetData() ?: return
    val (path: String, pathUri: String) = input

    val localActivity: AppCompatActivity = LocalAppCompatActivity.current
    val localResources: Resources = LocalResources.current
    val localClipboard: Clipboard = LocalClipboard.current

    val coroutineScope: CoroutineScope = rememberCoroutineScope()

    SelectionDialogBottomSheet(
        sheetState = sheetState,
        options = PhotoPathQuickActions.allOptions(path),
        optionName = { option: PhotoPathQuickActions -> localResources.getString(option.title) },
        onOptionSelect = { option: PhotoPathQuickActions ->
            when (option) {
                is PhotoPathQuickActions.Copy -> {
                    coroutineScope.launch {
                        localClipboard.setClipEntry(ClipData.newPlainText("", path).toClipEntry())
                    }
                }

                is PhotoPathQuickActions.Open -> {
                    val treeUri: Uri = pathUri.substringBeforeLast("/").toUri()
                    val intent = Intent(Intent.ACTION_VIEW)
                        .setDataAndType(treeUri.toFolderDocumentUri(), "vnd.android.document/directory")
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                    localActivity.startActivity(intent)
                }
            }
        },
    )
}

private sealed class PhotoPathQuickActions(
    @StringRes val title: Int,
) {

    abstract val path: String

    data class Copy(
        override val path: String,
    ) : PhotoPathQuickActions(title = R.string.photo_widget_viewer_quick_actions_copy)

    data class Open(
        override val path: String,
    ) : PhotoPathQuickActions(title = R.string.photo_widget_viewer_quick_actions_open_folder)

    companion object {

        fun allOptions(path: String): List<PhotoPathQuickActions> = listOf(
            Copy(path),
            Open(path),
        )
    }
}
