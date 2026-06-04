@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.fibelatti.photowidget.backup

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.model.LocalPhoto
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.model.getPhotoPath
import com.fibelatti.photowidget.platform.showMaterialAlertDialog
import com.fibelatti.photowidget.ui.AsyncPhotoViewer
import com.fibelatti.photowidget.ui.LoadingIndicator
import com.fibelatti.photowidget.ui.icons.AppIcons
import com.fibelatti.photowidget.ui.icons.Back
import com.fibelatti.photowidget.ui.icons.Export
import com.fibelatti.photowidget.ui.icons.Import
import com.fibelatti.photowidget.ui.icons.Warning
import com.fibelatti.ui.preview.PreviewAll
import com.fibelatti.ui.theme.ExtendedTheme

@Composable
fun PhotoWidgetBackupScreen(
    onNavClick: () -> Unit,
    onRestoreClick: (PhotoWidget) -> Unit,
    backupViewModel: PhotoWidgetBackupViewModel = hiltViewModel(),
) {
    val state: PhotoWidgetBackupViewModel.State = backupViewModel.state

    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result: ActivityResult ->
            val destinationUri: Uri? = result.data?.data
            if (result.resultCode == RESULT_OK && destinationUri != null) {
                backupViewModel.exportBackup(destinationUri = destinationUri)
            } else {
                backupViewModel.deletePreparedBackup()
            }
        },
    )

    val backupPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            if (uri != null) backupViewModel.restoreFromBackup(uri)
        },
    )

    val localContext: Context = LocalContext.current

    PhotoWidgetBackupScreen(
        onNavClick = onNavClick,
        isProcessing = state.isProcessing,
        onCreateBackupClick = backupViewModel::createBackup,
        onRestoreFromBackupClick = { backupPickerLauncher.launch("application/zip") },
        widgets = state.restoredWidgets,
        onRestoreClick = onRestoreClick,
    )

    SideEffect(state.preparedBackupFile) {
        state.preparedBackupFile?.let { file ->
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/zip"
                putExtra(Intent.EXTRA_TITLE, file.name)
            }
            saveFileLauncher.launch(intent)
        }
    }

    SideEffect(state.messages) {
        state.messages.firstOrNull()?.let { message ->
            when (message) {
                is PhotoWidgetBackupViewModel.State.Message.BackupExportedSuccessfully -> {
                    Toast.makeText(localContext, R.string.backup_feedback_success, Toast.LENGTH_SHORT)
                        .show()
                }

                is PhotoWidgetBackupViewModel.State.Message.BackupFailed -> {
                    localContext.showMaterialAlertDialog {
                        setMessage(R.string.backup_feedback_error)
                        setPositiveButton(R.string.photo_widget_action_got_it) { _, _ -> }
                    }
                }

                is PhotoWidgetBackupViewModel.State.Message.RestoreBackupFailed -> {
                    localContext.showMaterialAlertDialog {
                        setMessage(R.string.backup_feedback_restore_error)
                        setPositiveButton(R.string.photo_widget_action_got_it) { _, _ -> }
                    }
                }
            }

            backupViewModel.messageHandled(message)
        }
    }
}

@Composable
fun PhotoWidgetBackupScreen(
    onNavClick: () -> Unit,
    isProcessing: Boolean,
    onCreateBackupClick: () -> Unit,
    onRestoreFromBackupClick: () -> Unit,
    widgets: List<PhotoWidget>,
    onRestoreClick: (PhotoWidget) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        val blurRadius by animateDpAsState(
            targetValue = if (isProcessing) 10.dp else 0.dp,
            label = "ProcessingBlur",
        )

        Scaffold(
            modifier = Modifier.blur(radius = blurRadius),
            topBar = {
                TopAppBar(
                    title = {
                        Text(text = stringResource(id = R.string.photo_widget_home_backup))
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onNavClick,
                            shapes = IconButtonDefaults.shapes(),
                        ) {
                            Icon(
                                imageVector = AppIcons.Back,
                                contentDescription = null,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                )
            },
            contentWindowInsets = WindowInsets.safeDrawing,
        ) { contentPadding ->
            PhotoWidgetBackupContent(
                onCreateBackupClick = onCreateBackupClick,
                onRestoreFromBackupClick = onRestoreFromBackupClick,
                widgets = widgets,
                onRestoreClick = onRestoreClick,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            )
        }

        AnimatedVisibility(
            visible = isProcessing,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                LoadingIndicator(
                    modifier = Modifier.size(72.dp),
                )
            }
        }
    }
}

@Composable
private fun PhotoWidgetBackupContent(
    onCreateBackupClick: () -> Unit,
    onRestoreFromBackupClick: () -> Unit,
    widgets: List<PhotoWidget>,
    onRestoreClick: (PhotoWidget) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        BackupInstruction(
            icon = rememberVectorPainter(AppIcons.Export),
            text = stringResource(R.string.backup_explanation_1),
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))

        BackupInstruction(
            icon = rememberVectorPainter(AppIcons.Import),
            text = stringResource(R.string.backup_explanation_2),
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))

        BackupInstruction(
            icon = rememberVectorPainter(AppIcons.Warning),
            text = stringResource(R.string.backup_explanation_3),
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onCreateBackupClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp),
            shapes = ButtonDefaults.shapes(),
        ) {
            Text(text = stringResource(R.string.backup_create))
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onRestoreFromBackupClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp),
            shapes = ButtonDefaults.shapes(),
        ) {
            Text(text = stringResource(R.string.backup_restore))
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (widgets.isEmpty()) return@Column

        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.backup_items_title),
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleMedium,
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(widgets, key = { widget -> widget.photos.firstOrNull()?.photoId ?: widget.hashCode() }) { widget ->
                RestoredWidgetItem(
                    photoWidget = widget,
                    onRestoreClick = { onRestoreClick(widget) },
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun BackupInstruction(
    icon: Painter,
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
        )

        Text(
            text = AnnotatedString.fromHtml(text),
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.bodyLargeEmphasized,
        )
    }
}

@Composable
private fun RestoredWidgetItem(
    photoWidget: PhotoWidget,
    onRestoreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier
            .width(160.dp)
            .height(240.dp)
            .clip(shape = CardDefaults.elevatedShape),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        val items: List<LocalPhoto> = remember(photoWidget.photos) {
            photoWidget.photos.take(9)
        }
        val columnCount: Int = when {
            items.size == 1 -> 1
            items.size % 2 == 0 -> 2
            else -> 3
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(columnCount),
            modifier = Modifier
                .padding(16.dp)
                .weight(1f)
                .aspectRatio(1f)
                .align(Alignment.CenterHorizontally)
                .clip(MaterialTheme.shapes.medium),
            userScrollEnabled = false,
        ) {
            items(items, key = { it.photoId }) { localPhoto ->
                AsyncPhotoViewer(
                    data = localPhoto.getPhotoPath(),
                    isLoading = false,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .aspectRatio(1f),
                )
            }
        }

        FilledTonalButton(
            onClick = onRestoreClick,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .align(Alignment.CenterHorizontally),
            shapes = ButtonDefaults.shapes(),
        ) {
            Text(text = stringResource(R.string.backup_item_restore))
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// region Previews

@PreviewAll
@Composable
private fun PhotoWidgetBackupScreenPreview() {
    ExtendedTheme {
        PhotoWidgetBackupScreen(
            onNavClick = {},
            isProcessing = false,
            onCreateBackupClick = {},
            onRestoreFromBackupClick = {},
            widgets = emptyList(),
            onRestoreClick = {},
        )
    }
}

@PreviewAll
@Composable
private fun PhotoWidgetBackupScreenBackupSelectedPreview() {
    ExtendedTheme {
        PhotoWidgetBackupScreen(
            onNavClick = {},
            isProcessing = false,
            onCreateBackupClick = {},
            onRestoreFromBackupClick = {},
            widgets = List(size = 3) {
                PhotoWidget(
                    photos = List(size = 10) { index ->
                        LocalPhoto(photoId = "photo-$index")
                    },
                )
            },
            onRestoreClick = {},
        )
    }
}

// endregion Previews
