package com.fibelatti.photowidget.licenses

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.platform.AppTheme
import com.mikepenz.aboutlibraries.ui.compose.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.LibraryDefaults
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OssLicensesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                OssLicensesScreen()
            }
        }
    }

    @Composable
    @OptIn(ExperimentalFoundationApi::class)
    private fun OssLicensesScreen() {
        LibrariesContainer(
            modifier = Modifier.fillMaxSize(),
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
                            .fillMaxWidth()
                            .background(color = MaterialTheme.colorScheme.background),
                    ) {
                        IconButton(onClick = { finish() }) {
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
}
