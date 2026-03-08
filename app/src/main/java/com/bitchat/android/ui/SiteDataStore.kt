package com.bitchat.android.ui

import android.content.Context

object SiteDataStore {
    var siteConfig: SiteConfig? = null
        private set
    var equipment: List<EquipmentLocation> = emptyList()
        private set

    fun load(context: Context) {
        siteConfig = SiteDataSyncService.loadSiteConfig(context)
        equipment = SiteDataSyncService.loadEquipmentLocations(context)
    }

    suspend fun refresh(context: Context) {
        SiteDataSyncService.sync(context)
        load(context)
    }
}
