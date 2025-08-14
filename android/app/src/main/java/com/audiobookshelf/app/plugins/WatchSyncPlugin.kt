package com.audiobookshelf.app.plugins

import android.util.Log
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@CapacitorPlugin(name = "WatchSync")
class WatchSyncPlugin : Plugin() {

  private lateinit var downloader: FileDownloader
  private lateinit var wearOsSyncService: WatchSyncService // D of SOLID: Depend on the abstraction

  override fun load() {
    super.load()
    Log.d("WatchSyncPlugin", "WatchSyncPlugin loaded successfully.")
    downloader = FileDownloader(context)
    wearOsSyncService = WearOsSyncService(context) // Create an instance of our concrete implementation
  }

  @PluginMethod
  fun syncToWatch(call: PluginCall) {
    val itemId = call.getString("itemId") ?: ""
    val downloadUrl = call.getString("downloadUrl") ?: ""
    val token = call.getString("token") ?: ""

    if (downloadUrl.isEmpty() || token.isEmpty()) {
      call.reject("Download URL or token is missing.")
      return
    }

    CoroutineScope(Dispatchers.IO).launch {
      // First, find connected watches.
      val watches = wearOsSyncService.findConnectedWatches()
      if (watches.isEmpty()) {
        Log.d("WatchSyncPlugin", "No watches found.")
        call.reject("No watches found.")
        return@launch
      }

      Log.d("WatchSyncPlugin", "Found watches: $watches")

      // For now, we just log the watches. Next, we will download the file.
      val destinationFileName = "$itemId.m4a"
      val filePath = downloader.downloadFile(downloadUrl, token, destinationFileName)

      if (filePath != null) {
        // In the future, we will pass this filePath to the WatchSyncService.
        Log.d("WatchSyncPlugin", "File ready for watch sync at: $filePath")
        call.resolve()
      } else {
        call.reject("File download failed.")
      }
    }
  }
}
