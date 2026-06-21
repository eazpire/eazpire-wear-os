package com.eazpire.wear.os.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.eazpire.wear.core.api.WearPlayerApi
import com.eazpire.wear.core.auth.SecureTokenStore
import com.eazpire.wear.os.R
import com.eazpire.wear.os.health.WatchStepCounter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun MoveDashboard(tokenStore: SecureTokenStore) {
    val context = LocalContext.current
    val api = remember(tokenStore.getJwt()) { WearPlayerApi(jwt = tokenStore.getJwt()) }
    val stepCounter = remember { WatchStepCounter(context) }
    var steps by remember { mutableLongStateOf(0L) }
    var eazToday by remember { mutableLongStateOf(0L) }
    var claimAvailable by remember { mutableStateOf(false) }
    var syncing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun refresh() {
        scope.launch {
            runCatching {
                val status = api.moveToEarnStatusModel()
                steps = status.stepsToday
                eazToday = status.eazEarnedToday
                claimAvailable = status.dailyClaimAvailable
            }.onFailure { error = it.message }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.steps_today), style = MaterialTheme.typography.caption2)
        Text(steps.toString(), style = MaterialTheme.typography.title1)
        Text(
            "${stringResource(R.string.eaz_today)}: $eazToday",
            style = MaterialTheme.typography.body2,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = {
                scope.launch {
                    syncing = true
                    error = null
                    runCatching {
                        val local = runCatching { stepCounter.stepsFlow().first() }.getOrDefault(0f)
                        api.moveToEarnSyncSteps(local.toLong())
                        refresh()
                    }.onFailure { error = it.message }
                    syncing = false
                }
            },
            enabled = !syncing,
            modifier = Modifier.fillMaxWidth(0.85f),
        ) {
            Text(if (syncing) stringResource(R.string.loading) else stringResource(R.string.sync_steps))
        }
        if (claimAvailable) {
            Button(
                onClick = {
                    scope.launch {
                        runCatching { api.wearEarnClaim("move_daily") }
                        refresh()
                    }
                },
                modifier = Modifier.fillMaxWidth(0.85f),
            ) { Text(stringResource(R.string.daily_claim)) }
        }
        error?.let {
            Text(it, style = MaterialTheme.typography.caption3, textAlign = TextAlign.Center)
        }
    }
}
