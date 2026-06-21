package com.eazpire.wear.os.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.eazpire.wear.os.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(900)
        onFinished()
    }
    androidx.wear.compose.foundation.layout.Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.splash_title),
            style = MaterialTheme.typography.title2,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun PairingScreen() {
    androidx.wear.compose.foundation.layout.Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.pairing_title),
            style = MaterialTheme.typography.title3,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.pairing_hint),
            style = MaterialTheme.typography.body2,
            textAlign = TextAlign.Center,
            modifier = Modifier,
        )
        Text(
            text = stringResource(R.string.waiting),
            style = MaterialTheme.typography.caption2,
        )
    }
}
