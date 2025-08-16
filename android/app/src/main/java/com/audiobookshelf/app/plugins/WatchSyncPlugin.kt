package com.audiobookshelf.app.plugins

import android.util.Log
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@CapacitorPlugin(name = "WatchSync")
class WatchSyncPlugin : Plugin() {

  private lateinit var downloader: FileDownloader
  private lateinit var wearOsSyncService: WatchSyncService

  override fun load() {
    super.load()
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
      try {
        val watches = wearOsSyncService.findConnectedWatches()
        if (watches.isEmpty()) {
          call.reject("No watches found.")
          return@launch
        }
        val targetWatch = watches.first()

        val destinationFileName = "$itemId.m4a"
        val destinationFile = File(context.cacheDir, destinationFileName)
        val filePath = if (destinationFile.exists()) {
          Log.d("WatchSyncPlugin", "File already in cache.")
          destinationFile.absolutePath
        } else {
          downloader.downloadFile(downloadUrl, token, destinationFileName)
        }

        if (filePath != null) {
          Log.d("WatchSyncPlugin", "Starting sync to ${targetWatch.name}...")
          // Call the new, simpler syncBook method.
          wearOsSyncService.syncBook(targetWatch, filePath, itemId)
          Log.d("WatchSyncPlugin", "Sync handed off to system successfully!")
          call.resolve()
        } else {
          call.reject("File download failed.")
        }
      } catch (e: Exception) {
        Log.e("WatchSyncPlugin", "Sync failed", e)
        call.reject("Sync failed: ${e.message}")
      }
    }
  }
}
