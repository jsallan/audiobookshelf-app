package com.audiobookshelf.app.plugins

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await
import java.io.File

class WearOsSyncService(context: Context) : WatchSyncService {

  private val nodeClient = Wearable.getNodeClient(context)
  // The DataClient is the modern API for syncing data and assets.
  private val dataClient = Wearable.getDataClient(context)

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

  // The syncBook method is now a simple suspend function.
  // It no longer returns a Flow, as the system handles the transfer progress.
  override suspend fun syncBook(watch: WatchDevice, filePath: String, bookId: String) {
    try {
      val file = File(filePath)
      if (!file.exists()) {
        throw Exception("File to sync does not exist at path: $filePath")
      }

      // Create an "Asset" from the file. An asset is a blob of binary data.
      val fileAsset = Asset.createFromFd(android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_ONLY))

      // Create a Data Map Request. This is like an envelope for our data.
      // The path is a unique identifier for this type of data.
      val request = PutDataMapRequest.create("/audiobook/$bookId").apply {
        // Attach the file asset to the request with a key.
        dataMap.putAsset("audio_file", fileAsset)
        // Add a timestamp to ensure this data item is always seen as "new"
        dataMap.putLong("timestamp", System.currentTimeMillis())
      }

      Log.d("WearOsSyncService", "Putting data item with asset for book: $bookId")
      // Hand the request off to the system. It will handle the transfer reliably.
      dataClient.putDataItem(request.asPutDataRequest().setUrgent()).await()
      Log.d("WearOsSyncService", "Successfully put data item for book: $bookId")

    } catch (e: Exception) {
      Log.e("WearOsSyncService", "Failed to sync book with DataClient", e)
      // Re-throw the exception so the plugin can catch it.
      throw e
    }
  }
}
