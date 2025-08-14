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

  override fun load() {
    super.load()
    Log.d("WatchSyncPlugin", "WatchSyncPlugin loaded successfully.")
    downloader = FileDownloader(context)
  }

  @PluginMethod
  fun syncToWatch(call: PluginCall) {
    val itemId = call.getString("itemId") ?: ""
    val downloadUrl = call.getString("downloadUrl") ?: ""
    val token = call.getString("token") ?: "" // Get the token from the call

    if (downloadUrl.isEmpty() || token.isEmpty()) {
      call.reject("Download URL or token is missing.")
      return
    }

    CoroutineScope(Dispatchers.IO).launch {
      val destinationFileName = "$itemId.m4a"
      // Pass the token to the downloader
      val filePath = downloader.downloadFile(downloadUrl, token, destinationFileName)

      if (filePath != null) {
        Log.d("WatchSyncPlugin", "File ready for watch sync at: $filePath")
        call.resolve()
      } else {
        call.reject("File download failed.")
      }
    }
  }
}
