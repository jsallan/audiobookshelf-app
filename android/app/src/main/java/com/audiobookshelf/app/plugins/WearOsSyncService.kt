package com.audiobookshelf.app.plugins

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

/**
 * SRP: This class's ONLY responsibility is to manage communication with Wear OS devices.
 * It is a concrete implementation of our WatchSyncService interface.
 */
class WearOsSyncService(context: Context) : WatchSyncService {

  // The NodeClient is the main entry point for finding connected watches.
  private val nodeClient = Wearable.getNodeClient(context)

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

  override fun syncBook(watch: WatchDevice, filePath: String, bookId: String): Flow<Int> {
    // We will implement the file transfer logic in the next step.
    TODO("Not yet implemented")
  }
}
