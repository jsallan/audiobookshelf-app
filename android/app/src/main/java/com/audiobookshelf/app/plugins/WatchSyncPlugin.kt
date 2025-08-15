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
import java.io.File

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
      val targetWatch = watches.first()

      // --- CACHING LOGIC ---
      val destinationFileName = "$itemId.m4a"
      val destinationFile = File(context.cacheDir, destinationFileName)
      var filePath: String?

      if (destinationFile.exists()) {
        Log.d("WatchSyncPlugin", "File already exists in cache. Skipping download.")
        filePath = destinationFile.absolutePath
      } else {
        Log.d("WatchSyncPlugin", "File not in cache. Starting download.")
        filePath = downloader.downloadFile(downloadUrl, token, destinationFileName)
      }
      // --- END CACHING LOGIC ---

      if (filePath != null) {
        Log.d("WatchSyncPlugin", "Download complete. Starting sync to ${targetWatch.name}...")
        wearOsSyncService.syncBook(targetWatch, filePath, itemId)
          .catch { e ->
            Log.e("WatchSyncPlugin", "Sync failed", e)
            call.reject("Sync failed: ${e.message}")
          }
          .collect { progress ->
            Log.d("WatchSyncPlugin", "Sync progress: $progress%")
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
