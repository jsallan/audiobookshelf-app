package com.audiobookshelf.app.plugins

import kotlinx.coroutines.flow.Flow

/**
 * This is the corrected interface. The syncBook method is now a suspend function,
 * matching the DataClient implementation.
 */
interface WatchSyncService {
  suspend fun findConnectedWatches(): List<WatchDevice>

  // This function is now a suspend function and does not return a Flow.
  suspend fun syncBook(watch: WatchDevice, filePath: String, bookId: String)
}
