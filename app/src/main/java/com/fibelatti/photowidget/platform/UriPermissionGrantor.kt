package com.fibelatti.photowidget.platform

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import dagger.Reusable
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

@Reusable
class UriPermissionGrantor @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val launcherPackagesCache: List<String> by lazy { getLauncherPackages() }

    suspend operator fun invoke(path: String): Uri? = withContext(Dispatchers.IO) {
        if (launcherPackagesCache.isEmpty()) {
            Timber.w("No launcher packages found, unable to generate URI.")
            return@withContext null
        }

        val file = File(path)

        if (!file.exists()) {
            Timber.w("File (path=$path) does not exist, unable to generate URI.")
            return@withContext null
        }

        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        Timber.d("New URI (path=$path): $uri")

        for (pkg in launcherPackagesCache) {
            context.grantUriPermission(pkg, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            Timber.d("Granted URI permission for package: $pkg")
        }

        return@withContext uri
    }

    private fun getLauncherPackages(): List<String> {
        val intent: Intent = Intent("android.intent.action.MAIN")
            .addCategory("android.intent.category.HOME")

        val samsungPackages: List<String> = listOf(
            "com.samsung.android.goodlock",
            "com.samsung.systemui.lockstar",
        )

        return context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .mapNotNull { resolveInfo -> resolveInfo.activityInfo?.packageName }
            .distinct()
            .ifEmpty { return emptyList() }
            .plus(
                if (Build.MANUFACTURER.equals("samsung", ignoreCase = true)) {
                    samsungPackages
                } else {
                    emptyList()
                },
            )
    }
}
