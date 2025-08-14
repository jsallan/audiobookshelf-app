package com.audiobookshelf.app.plugins

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class FileDownloader(private val context: Context) {

  // The method now accepts a token parameter
  suspend fun downloadFile(downloadUrl: String, token: String, destinationFileName: String): String? {
    return try {
      Log.d("FileDownloader", "Starting download from: $downloadUrl")
      val url = URL(downloadUrl)
      val connection = url.openConnection() as HttpURLConnection

      // Add the Authorization header to the request
      connection.setRequestProperty("Authorization", "Bearer $token")

      connection.connect()

      if (connection.responseCode != HttpURLConnection.HTTP_OK) {
        Log.e("FileDownloader", "Server returned HTTP ${connection.responseCode} ${connection.responseMessage}")
        return null
      }

      val destinationFile = File(context.cacheDir, destinationFileName)
      val outputStream = FileOutputStream(destinationFile)
      val inputStream = connection.inputStream

      val buffer = ByteArray(1024)
      var len: Int
      while (inputStream.read(buffer).also { len = it } > 0) {
        outputStream.write(buffer, 0, len)
      }

      outputStream.close()
      inputStream.close()
      connection.disconnect()

      Log.d("FileDownloader", "Download complete. File saved to: ${destinationFile.absolutePath}")
      destinationFile.absolutePath
    } catch (e: Exception) {
      Log.e("FileDownloader", "Error during download", e)
      null
    }
  }
}
