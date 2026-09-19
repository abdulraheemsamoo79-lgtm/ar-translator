package com.arprime.translator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.arprime.translator.ui.HistoryScreen
import com.arprime.translator.ui.TranslatorScreen
import com.arprime.translator.ui.UpdateAvailableDialog
import com.arprime.translator.ui.theme.ArTranslatorTheme
import com.arprime.translator.util.Prefs
import com.arprime.translator.util.UpdateChecker
import com.arprime.translator.util.UpdateInfo
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            val darkTheme by Prefs(this).darkTheme.collectAsState(initial = false)
            var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
            val scope = rememberCoroutineScope()
            val prefs = remember { Prefs(this) }

            // Silent update check on launch — only bothers the user if a newer
            // versionCode than BuildConfig.VERSION_CODE, and than what they
            // already dismissed, is on the server.
            LaunchedEffect(Unit) {
                val result = UpdateChecker.checkForUpdate(BuildConfig.VERSION_CODE)
                if (result != null) {
                    val dismissed = prefs.dismissedVersionCode()
                    if (result.forceUpdate || result.versionCode > dismissed) {
                        updateInfo = result
                    }
                }
            }

            ArTranslatorTheme(darkTheme = darkTheme) {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "translate") {
                    composable("translate") {
                        TranslatorScreen(onOpenHistory = { navController.navigate("history") })
                    }
                    composable("history") {
                        HistoryScreen(onBack = { navController.popBackStack() })
                    }
                }

                updateInfo?.let { info ->
                    UpdateAvailableDialog(
                        info = info,
                        onDismiss = {
                            scope.launch { prefs.dismissUpdate(info.versionCode) }
                            updateInfo = null
                        }
                    )
                }
            }
        }
    }
}
