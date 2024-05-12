package com.fibelatti.photowidget.preferences

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.fibelatti.photowidget.platform.AppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WidgetDefaultsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                WidgetDefaultsScreen(
                    onNavClick = { finish() },
                )
            }
        }
    }
}
