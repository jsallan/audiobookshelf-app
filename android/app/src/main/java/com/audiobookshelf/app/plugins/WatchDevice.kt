package com.audiobookshelf.app.plugins

/**
 * A simple data class to represent a connected watch in a platform-agnostic way.
 */
data class WatchDevice(
  val id: String,
  val name: String,
  val isNearby: Boolean
)
