package com.arprime.translator.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.arprime.translator.util.UpdateInfo

@Composable
fun UpdateAvailableDialog(info: UpdateInfo, onDismiss: () -> Unit) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = { if (!info.forceUpdate) onDismiss() },
        title = { Text("Update available \u2014 v${info.versionName}") },
        text = { Text(info.changelog) },
        confirmButton = {
            TextButton(onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(info.downloadUrl)))
            }) { Text("Download latest version") }
        },
        dismissButton = {
            if (!info.forceUpdate) {
                TextButton(onClick = onDismiss) { Text("Later") }
            }
        }
    )
}
