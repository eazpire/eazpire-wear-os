package com.eazpire.wear.os.auth

import android.content.Context
import com.eazpire.wear.core.auth.SecureTokenStore
import com.eazpire.wear.core.auth.WearPlayerAuthPaths
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import org.json.JSONObject

class WearAuthListenerService : WearableListenerService() {
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type != DataEvent.TYPE_CHANGED) continue
            val path = event.dataItem.uri.path ?: continue
            if (path != WearPlayerAuthPaths.DATA_PATH) continue
            val payload = DataMapItem.fromDataItem(event.dataItem).dataMap.getString("payload") ?: continue
            WearAuthBootstrap.applyPayload(applicationContext, payload)
        }
    }
}

object WearAuthBootstrap {
    private val listeners = mutableListOf<() -> Unit>()

    fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    fun applyPayload(context: Context, payload: String) {
        runCatching {
            val json = JSONObject(payload)
            val jwt = json.optString("jwt")
            val ownerId = json.optString("owner_id")
            if (jwt.isBlank() || ownerId.isBlank()) return
            SecureTokenStore.get(context).saveJwt(jwt, ownerId)
            listeners.forEach { it.invoke() }
        }
    }
}
