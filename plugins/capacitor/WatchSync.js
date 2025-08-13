import { registerPlugin, WebPlugin } from '@capacitor/core'

class WatchSyncWeb extends WebPlugin {
  // This is the web implementation of our plugin.
  // Since we can't sync to a watch from a desktop browser,
  // this method will just log a warning.
  async syncToWatch(options) {
    console.warn('WatchSync is not available on the web. Called with:', options)
    return
  }
}

// This registers our plugin with Capacitor.
// It links the name "WatchSync" to our native Kotlin code
// and provides the WatchSyncWeb class as the fallback.
const WatchSync = registerPlugin('WatchSync', {
  web: () => new WatchSyncWeb()
})

export { WatchSync }
