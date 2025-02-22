package com.fibelatti.photowidget.licenses

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.platform.AppTheme
import com.fibelatti.ui.foundation.copy
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.ui.compose.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.LibraryDefaults
import com.mikepenz.aboutlibraries.util.withJson
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OssLicensesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                OssLicensesScreen(
                    onBackNavClick = ::finish,
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun OssLicensesScreen(
    onBackNavClick: () -> Unit,
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = WindowInsets.safeDrawing.asPaddingValues(),
) {
    LibrariesContainer(
        modifier = modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background)
            .padding(top = paddingValues.calculateTopPadding()),
        librariesBlock = { ctx ->
            Libs.Builder().withJson(ctx, R.raw.aboutlibraries).build()
        },
        contentPadding = paddingValues.copy(top = 0.dp),
        colors = LibraryDefaults.libraryColors(
            backgroundColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            badgeBackgroundColor = MaterialTheme.colorScheme.primaryContainer,
            badgeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            dialogConfirmButtonColor = MaterialTheme.colorScheme.primary,
        ),
        header = {
            stickyHeader {
                Row(
                    modifier = Modifier
                        .background(color = MaterialTheme.colorScheme.background)
                        .fillMaxWidth(),
                ) {
                    IconButton(onClick = onBackNavClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        },
    )
}
