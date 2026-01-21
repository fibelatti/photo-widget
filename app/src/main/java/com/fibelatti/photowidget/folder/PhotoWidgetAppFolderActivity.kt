package com.fibelatti.photowidget.folder

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import com.fibelatti.photowidget.model.InstalledApp
import com.fibelatti.photowidget.model.PhotoWidgetTapAction
import com.fibelatti.photowidget.platform.AppTheme
import com.fibelatti.photowidget.platform.getDynamicAttributeColor
import com.fibelatti.photowidget.platform.getInstalledApp
import com.fibelatti.photowidget.platform.getLaunchIntent
import com.fibelatti.photowidget.platform.intentExtras
import com.fibelatti.photowidget.platform.setIdentifierCompat
import com.fibelatti.photowidget.ui.WarningSign
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PhotoWidgetAppFolderActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                ScreenContent(
                    apps = intent.appFolderTapAction.shortcuts.mapNotNull(::getInstalledApp),
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

    private fun launchApp(app: InstalledApp) {
        val launchIntent: Intent? = getLaunchIntent(packageName = app.appPackage)

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
    apps: List<InstalledApp>,
    onEditWidgetClick: () -> Unit,
    onAppClick: (InstalledApp) -> Unit,
    onBackgroundClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val localContext = LocalContext.current

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
        if (apps.isEmpty()) {
            WarningSign(
                text = stringResource(R.string.photo_widget_app_folder_empty),
                textStyle = MaterialTheme.typography.bodyLarge,
                showDismissButton = true,
                dismissButtonText = stringResource(R.string.photo_widget_action_continue),
                onDismissClick = onEditWidgetClick,
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
                val rows: List<List<InstalledApp>> = remember(apps) {
                    apps.chunked(
                        size = when {
                            apps.size <= 4 -> 2
                            apps.size <= 6 || apps.size == 9 -> 3
                            else -> 4
                        },
                    )
                }

                for (row in rows) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        for (app in row) {
                            AppItem(
                                app = app,
                                modifier = Modifier
                                    .clip(shape = MaterialTheme.shapes.small)
                                    .clickable(onClick = { onAppClick(app) }),
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
    app: InstalledApp,
    modifier: Modifier = Modifier,
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(
        modifier = modifier
            .width(72.dp)
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            bitmap = app.appIcon.toBitmap().asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.size(48.dp),
        )

        Text(
            text = app.appLabel,
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
