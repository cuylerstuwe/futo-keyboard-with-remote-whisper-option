package org.futo.inputmethod.latin.uix.settings.pages

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import org.futo.voiceinput.shared.ui.WhisperSettingsScreen

/**
 * Wrapper for the WhisperSettingsScreen to integrate it into the app's navigation.
 */
@Composable
fun RemoteWhisperScreen(navController: NavHostController) {
    WhisperSettingsScreen()
}
