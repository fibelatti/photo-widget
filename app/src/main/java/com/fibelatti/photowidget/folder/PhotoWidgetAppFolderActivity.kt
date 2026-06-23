package com.fibelatti.photowidget.folder

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Typeface
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.configure.PhotoWidgetConfigureActivity
import com.fibelatti.photowidget.configure.appWidgetId
import com.fibelatti.photowidget.model.AppFolderResolvedEntry
import com.fibelatti.photowidget.model.PhotoWidgetTapAction
import com.fibelatti.photowidget.platform.AppTheme
import com.fibelatti.photowidget.platform.disableWindowNavigationBarContrastEnforced
import com.fibelatti.photowidget.platform.enableEdgeToEdgeTransparent
import com.fibelatti.photowidget.platform.getDynamicAttributeColor
import com.fibelatti.photowidget.platform.getLaunchIntent
import com.fibelatti.photowidget.platform.intentExtras
import com.fibelatti.photowidget.platform.resolveAppFolderEntries
import com.fibelatti.photowidget.platform.setIdentifierCompat
import com.fibelatti.photowidget.ui.InformationalPanel
import com.fibelatti.photowidget.widget.AppShortcutLauncherActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class PhotoWidgetAppFolderActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdgeTransparent()
        disableWindowNavigationBarContrastEnforced()
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                ScreenContent(
                    shortcuts = intent.appFolderTapAction.shortcuts,
                    onEditWidgetClick = {
                        startActivity(
                            PhotoWidgetConfigureActivity.editWidgetIntent(
                                context = this,
                                appWidgetId = intent.appWidgetId,
                            ),
                        )
                        finish()
                    },
                    onAppClick = ::launchApp,
                    onBackgroundClick = ::finish,
                )
            }
        }
    }

    private fun launchApp(entry: AppFolderResolvedEntry) {
        val launchIntent: Intent? = if (entry.shortcut != null) {
            AppShortcutLauncherActivity.newIntent(
                context = this,
                packageName = entry.app.appPackage,
                shortcutId = entry.shortcut.id,
            )
        } else {
            getLaunchIntent(packageName = entry.app.appPackage)
        }

        if (launchIntent != null) {
            startActivity(launchIntent)
        }

        finish()
    }

    companion object {

        private var Intent.appFolderTapAction: PhotoWidgetTapAction.AppFolder by intentExtras()

        fun newIntent(
            context: Context,
            appWidgetId: Int,
            appFolderTapAction: PhotoWidgetTapAction.AppFolder,
        ): Intent {
            return Intent(context, PhotoWidgetAppFolderActivity::class.java).apply {
                setIdentifierCompat("$appWidgetId")
                this.appWidgetId = appWidgetId
                this.appFolderTapAction = appFolderTapAction
            }
        }
    }
}

@Composable
private fun ScreenContent(
    shortcuts: List<String>,
    onEditWidgetClick: () -> Unit,
    onAppClick: (AppFolderResolvedEntry) -> Unit,
    onBackgroundClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val localContext = LocalContext.current

    val entries: List<AppFolderResolvedEntry>? by produceState(initialValue = null, key1 = shortcuts) {
        value = withContext(Dispatchers.IO) { localContext.resolveAppFolderEntries(shortcuts) }
    }

    // Avoid flickering the empty state while the list is still loading
    val currentEntries: List<AppFolderResolvedEntry> = entries ?: return

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                onClick = onBackgroundClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            )
            .safeContentPadding(),
        contentAlignment = Alignment.Center,
    ) {
        if (currentEntries.isEmpty()) {
            InformationalPanel(
                text = stringResource(R.string.photo_widget_app_folder_empty),
                textStyle = MaterialTheme.typography.bodyLarge,
                showActionButton = true,
                actionButtonText = stringResource(R.string.photo_widget_action_continue),
                onActionButtonClick = onEditWidgetClick,
            )
        } else {
            val backgroundColor = Color(
                localContext.getDynamicAttributeColor(
                    com.google.android.material.R.attr.colorSurfaceContainer,
                ),
            )
            val labelColor = Color(
                localContext.getDynamicAttributeColor(
                    com.google.android.material.R.attr.colorOnSurface,
                ),
            )

            Column(
                modifier = Modifier
                    .background(shape = MaterialTheme.shapes.extraLarge, color = backgroundColor)
                    .padding(vertical = 24.dp, horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val rows: List<List<AppFolderResolvedEntry>> = remember(entries) {
                    currentEntries.chunked(
                        size = when {
                            currentEntries.size <= 4 -> 2
                            currentEntries.size <= 6 || currentEntries.size == 9 -> 3
                            else -> 4
                        },
                    )
                }

                for (row in rows) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        for (entry in row) {
                            AppItem(
                                entry = entry,
                                modifier = Modifier
                                    .clip(shape = MaterialTheme.shapes.small)
                                    .clickable(onClick = { onAppClick(entry) }),
                                labelColor = labelColor,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppItem(
    entry: AppFolderResolvedEntry,
    modifier: Modifier = Modifier,
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    val icon: Drawable = entry.shortcut?.icon ?: entry.app.appIcon
    val label: String = entry.shortcut?.label ?: entry.app.appLabel

    Column(
        modifier = modifier
            .width(72.dp)
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            bitmap = icon.toBitmap().asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.size(48.dp),
        )

        Text(
            text = label,
            modifier = Modifier.fillMaxWidth(),
            color = labelColor,
            fontSize = 12.sp,
            fontFamily = FontFamily(Typeface(android.graphics.Typeface.DEFAULT)),
            textAlign = TextAlign.Center,
            lineHeight = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
