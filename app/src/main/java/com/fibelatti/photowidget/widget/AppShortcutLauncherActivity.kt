package com.fibelatti.photowidget.widget

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.fibelatti.photowidget.platform.getAppShortcutIntent
import com.fibelatti.photowidget.platform.getLaunchIntent
import com.fibelatti.photowidget.platform.intentExtras
import timber.log.Timber

class AppShortcutLauncherActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val shortcutIntent: Intent? = getAppShortcutIntent(intent.shortcutPackageName, intent.shortcutId)
        if (shortcutIntent != null) {
            runCatching { startActivity(shortcutIntent) }.onFailure { e ->
                Timber.w(e, "Failed to launch shortcut, falling back to app launch")
                getLaunchIntent(intent.shortcutPackageName)?.let { runCatching { startActivity(it) } }
            }
        } else {
            getLaunchIntent(intent.shortcutPackageName)?.let { runCatching { startActivity(it) } }
        }

        finish()
    }

    companion object {

        private var Intent.shortcutPackageName: String by intentExtras()
        private var Intent.shortcutId: String by intentExtras()

        fun newIntent(context: Context, packageName: String, shortcutId: String): Intent {
            return Intent(context, AppShortcutLauncherActivity::class.java).apply {
                this.shortcutPackageName = packageName
                this.shortcutId = shortcutId
            }
        }
    }
}
