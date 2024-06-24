package utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

internal fun Context.openSystemSettings() {
    Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null)
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }.let {
        startActivity(it)
    }
}

internal fun Context.openNfcSettings() {
    Intent(
        Settings.ACTION_NFC_SETTINGS,
//        Uri.fromParts("package", packageName, null)
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }.let {
        startActivity(it)
    }
}
