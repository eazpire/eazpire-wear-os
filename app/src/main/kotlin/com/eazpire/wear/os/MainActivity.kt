package com.eazpire.wear.os

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.eazpire.wear.core.auth.SecureTokenStore
import com.eazpire.wear.os.auth.WearAuthBootstrap
import com.eazpire.wear.os.ui.MoveDashboard
import com.eazpire.wear.os.ui.PairingScreen
import com.eazpire.wear.os.ui.SplashScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tokenStore = SecureTokenStore.get(this)
        setContent {
            var showSplash by remember { mutableStateOf(true) }
            var loggedIn by remember { mutableStateOf(tokenStore.isLoggedIn()) }

            DisposableEffect(Unit) {
                val listener = {
                    loggedIn = tokenStore.isLoggedIn()
                }
                WearAuthBootstrap.addListener(listener)
                onDispose { WearAuthBootstrap.removeListener(listener) }
            }

            when {
                showSplash -> SplashScreen(onFinished = { showSplash = false })
                !loggedIn -> PairingScreen()
                else -> MoveDashboard(tokenStore)
            }
        }
    }
}
