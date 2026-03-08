package kr.jm.weddingledger.core.platform

object AndroidNotificationPermissionBridge {
    fun bindLauncher(block: (String) -> Unit) {
        AndroidNotificationPermissionRequester.bindLauncher(block)
    }

    fun unbindLauncher() {
        AndroidNotificationPermissionRequester.unbindLauncher()
    }

    fun onPermissionResult(granted: Boolean) {
        AndroidNotificationPermissionRequester.onPermissionResult(granted)
    }
}
