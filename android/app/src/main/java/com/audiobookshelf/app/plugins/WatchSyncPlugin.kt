package com.audiobookshelf.app.plugins

import android.util.Log
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin

@CapacitorPlugin(name = "WatchSync")
class WatchSyncPlugin : Plugin() {

  // This method is called when the plugin is first initialized by the app.
  override fun load() {
    super.load()
    Log.d("WatchSyncPlugin", "WatchSyncPlugin loaded successfully.") // <-- ADD THIS LOG
  }

  @PluginMethod
  fun syncToWatch(call: PluginCall) {
    val itemId = call.getString("itemId") ?: ""
    Log.d("WatchSyncPlugin", "SUCCESS: syncToWatch called for item: $itemId")
    call.resolve()
  }
}
