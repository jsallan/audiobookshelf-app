package com.audiobookshelf.app.plugins

import kotlinx.coroutines.flow.Flow

/**
 * L of SOLID: This interface is the "contract" or "blueprint" for any watch sync service.
 * Both Garmin and Wear OS services will implement this, allowing them to be used interchangeably.
 * This adheres to the Liskov Substitution Principle.
 *
 * D of SOLID: Our high-level plugin will depend on this abstraction, not on the concrete
 * low-level implementations, adhering to the Dependency Inversion Principle.
 */
interface WatchSyncService {
  /**
   * Finds and lists all connected watches of this type.
   */
  suspend fun findConnectedWatches(): List<WatchDevice>

  /**
   * Starts the process of sending a book to a specific watch.
   * @param watch The device to send the file to.
   * @param filePath The path to the downloaded audio file on the phone.
   * @param bookId The unique ID of the book being sent.
   * @return A Flow that emits the progress of the transfer as a percentage (0-100).
   */
  fun syncBook(watch: WatchDevice, filePath: String, bookId: String): Flow<Int>
}
