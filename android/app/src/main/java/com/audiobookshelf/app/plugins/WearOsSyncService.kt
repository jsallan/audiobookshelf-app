package com.audiobookshelf.app.plugins

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileInputStream

class WearOsSyncService(context: Context) : WatchSyncService {

  private val nodeClient = Wearable.getNodeClient(context)
  // The ChannelClient is the API for sending large data streams like files.
  private val channelClient = Wearable.getChannelClient(context)

  override suspend fun findConnectedWatches(): List<WatchDevice> {
    return try {
      val nodes = nodeClient.connectedNodes.await()
      nodes.map { node ->
        WatchDevice(
          id = node.id,
          name = node.displayName,
          isNearby = node.isNearby
        )
      }
    } catch (e: Exception) {
      Log.e("WearOsSyncService", "Failed to find connected watches", e)
      emptyList()
    }
  }

  /**
   * This method now contains the logic to stream a file to the watch.
   */
  override fun syncBook(watch: WatchDevice, filePath: String, bookId: String): Flow<Int> = callbackFlow {
    try {
      val file = File(filePath)
      if (!file.exists()) {
        throw Exception("File to sync does not exist at path: $filePath")
      }

      // A "channel" is a persistent connection for streaming data.
      // The path acts as a unique identifier for the type of data being sent.
      val channel = channelClient.openChannel(watch.id, "/audiobook/$bookId").await()
      Log.d("WearOsSyncService", "Opened channel to ${watch.name} with path: ${channel.path}")

      // Get an output stream to write data to the watch.
      val outputStream = channelClient.getOutputStream(channel).await()
      val inputStream = FileInputStream(file)
      val totalSize = file.length()
      var bytesSent = 0L

      Log.d("WearOsSyncService", "Streaming file: $filePath ($totalSize bytes)")

      outputStream.use { stream ->
        val buffer = ByteArray(4096) // 4KB buffer
        var len: Int
        while (inputStream.read(buffer).also { len = it } > 0) {
          stream.write(buffer, 0, len)
          bytesSent += len
          // Calculate and send progress updates.
          val progress = ((bytesSent.toDouble() / totalSize.toDouble()) * 100).toInt()
          trySend(progress)
        }
      }

      Log.d("WearOsSyncService", "File stream complete. Closing channel.")
      channelClient.close(channel).await()
      trySend(100) // Ensure we end at 100%
      close() // Close the flow

    } catch (e: Exception) {
      Log.e("WearOsSyncService", "Failed to sync book", e)
      close(e) // Close the flow with an error
    }

    awaitClose {
      // This block is called when the flow is cancelled.
      // We can add cleanup logic here if needed in the future.
      Log.d("WearOsSyncService", "Sync flow is closing.")
    }
  }
}
