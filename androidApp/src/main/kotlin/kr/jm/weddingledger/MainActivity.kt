package kr.jm.weddingledger

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import kr.jm.weddingledger.core.platform.AndroidNotificationPermissionBridge
import kr.jm.weddingledger.feature.auth.data.platform.AndroidAuthActivityHolder

class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        AndroidNotificationPermissionBridge.onPermissionResult(granted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AndroidAuthActivityHolder.set(this)
        AndroidNotificationPermissionBridge.bindLauncher { permission ->
            notificationPermissionLauncher.launch(permission)
        }
        emitIncomingDeepLink(intent)

        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        emitIncomingDeepLink(intent)
    }

    override fun onDestroy() {
        AndroidAuthActivityHolder.clear(this)
        AndroidNotificationPermissionBridge.unbindLauncher()
        super.onDestroy()
    }

    private fun emitIncomingDeepLink(intent: Intent?) {
        val url = intent?.dataString ?: return
        pushIncomingDeepLink(url)
    }
}
