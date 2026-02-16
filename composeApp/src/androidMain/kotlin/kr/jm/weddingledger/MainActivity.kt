package kr.jm.weddingledger

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import kr.jm.weddingledger.core.platform.AndroidNotificationPermissionRequester

class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        AndroidNotificationPermissionRequester.onPermissionResult(granted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AndroidNotificationPermissionRequester.bindLauncher { permission ->
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
        AndroidNotificationPermissionRequester.unbindLauncher()
        super.onDestroy()
    }

    private fun emitIncomingDeepLink(intent: Intent?) {
        val url = intent?.dataString ?: return
        pushIncomingDeepLink(url)
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
