package com.fibelatti.photowidget.home

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.app.ShareCompat
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.configure.PhotoWidgetConfigureActivity
import com.fibelatti.photowidget.configure.aspectRatio
import com.fibelatti.photowidget.model.PhotoWidgetAspectRatio
import com.fibelatti.photowidget.platform.AppTheme
import com.fibelatti.photowidget.platform.SelectionDialog
import com.fibelatti.ui.foundation.stableListOf
import com.fibelatti.ui.foundation.toStableList
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HomeActivity : AppCompatActivity() {

    @Inject
    lateinit var userPreferencesStorage: UserPreferencesStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                HomeScreen(
                    onCreateNewWidgetClick = ::createNewWidget,
                    onAppearanceClick = ::showAppearancePicker,
                    onColorsClick = ::showAppColorsPicker,
                    onShareClick = ::shareApp,
                    onRateClick = ::rateApp,
                    onViewLicensesClick = ::viewOpenSourceLicenses,
                )
            }
        }
    }

    private fun createNewWidget(aspectRatio: PhotoWidgetAspectRatio) {
        val intent = Intent(this, PhotoWidgetConfigureActivity::class.java).apply {
            this.aspectRatio = aspectRatio
        }
        startActivity(intent)
    }

    private fun showAppearancePicker() {
        SelectionDialog.show(
            context = this,
            title = getString(R.string.photo_widget_home_appearance),
            options = Appearance.entries.toStableList(),
            optionName = { appearance ->
                getString(
                    when (appearance) {
                        Appearance.FOLLOW_SYSTEM -> R.string.preferences_appearance_follow_system
                        Appearance.LIGHT -> R.string.preferences_appearance_light
                        Appearance.DARK -> R.string.preferences_appearance_dark
                    },
                )
            },
            onOptionSelected = { newAppearance ->
                userPreferencesStorage.appearance = newAppearance

                val mode = when (newAppearance) {
                    Appearance.DARK -> AppCompatDelegate.MODE_NIGHT_YES
                    Appearance.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }

                AppCompatDelegate.setDefaultNightMode(mode)
            },
        )
    }

    private fun showAppColorsPicker() {
        SelectionDialog.show(
            context = this,
            title = getString(R.string.photo_widget_home_dynamic_colors),
            options = stableListOf(true, false),
            optionName = { value ->
                getString(
                    if (value) {
                        R.string.preferences_dynamic_colors_enabled
                    } else {
                        R.string.preferences_dynamic_colors_disabled
                    },
                )
            },
            onOptionSelected = { newValue ->
                userPreferencesStorage.dynamicColors = newValue

                ActivityCompat.recreate(this)
            },
        )
    }

    private fun shareApp() {
        ShareCompat.IntentBuilder(this)
            .setType("text/plain")
            .setChooserTitle(R.string.share_title)
            .setText(getString(R.string.share_text, APP_URL))
            .startChooser()
    }

    private fun rateApp() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(APP_URL)
            setPackage("com.android.vending")
        }

        startActivity(intent)
    }

    private fun viewOpenSourceLicenses() {
        OssLicensesMenuActivity.setActivityTitle(getString(R.string.photo_widget_licenses_title))
        startActivity(Intent(this, OssLicensesMenuActivity::class.java))
    }

    private companion object {

        private const val APP_URL = "https://play.google.com/store/apps/details?id=com.fibelatti.photowidget"
    }
}
