package com.audiobookshelf.app.plugins

import android.util.Log
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@CapacitorPlugin(name = "WatchSync")
class WatchSyncPlugin : Plugin() {

  private lateinit var downloader: FileDownloader
  private lateinit var wearOsSyncService: WatchSyncService

  override fun load() {
    super.load()
    Log.d("WatchSyncPlugin", "WatchSyncPlugin loaded successfully.")
    downloader = FileDownloader(context)
    wearOsSyncService = WearOsSyncService(context)
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
      val watches = wearOsSyncService.findConnectedWatches()
      if (watches.isEmpty()) {
        Log.d("WatchSyncPlugin", "No watches found.")
        call.reject("No watches found.")
        return@launch
      }
      Log.d("WatchSyncPlugin", "Found watches: $watches")

      // For now, let's just sync to the first watch we find.
      val targetWatch = watches.first()

      val destinationFileName = "$itemId.m4a"
      val filePath = downloader.downloadFile(downloadUrl, token, destinationFileName)

      if (filePath != null) {
        Log.d("WatchSyncPlugin", "Download complete. Starting sync to ${targetWatch.name}...")

        // Call the syncBook method and collect progress updates.
        wearOsSyncService.syncBook(targetWatch, filePath, itemId)
          .catch { e ->
            Log.e("WatchSyncPlugin", "Sync failed", e)
            call.reject("Sync failed: ${e.message}")
          }
          .collect { progress ->
            Log.d("WatchSyncPlugin", "Sync progress: $progress%")
            // In the future, we can send these progress events to the UI.
            if (progress == 100) {
              Log.d("WatchSyncPlugin", "Sync complete!")
              call.resolve()
            }
          }
      } else {
        call.reject("File download failed.")
      }
    }
  }
}
