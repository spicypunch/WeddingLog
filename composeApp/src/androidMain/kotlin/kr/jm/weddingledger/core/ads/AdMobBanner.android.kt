package kr.jm.weddingledger.core.ads

import android.content.pm.ApplicationInfo
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import kr.jm.weddingledger.core.config.BuildConfig

private const val AndroidTestBannerAdUnitId = "ca-app-pub-3940256099942544/9214589741"

@Composable
actual fun AdMobBanner(modifier: Modifier) {
    val context = LocalContext.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val selectedAdUnitId = remember(context) {
        if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            AndroidTestBannerAdUnitId
        } else {
            BuildConfig.ADMOB_ANDROID_BANNER_UNIT_ID.ifBlank { AndroidTestBannerAdUnitId }
        }
    }
    val adSize = remember(screenWidthDp, context) {
        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, screenWidthDp)
    }
    val adView = remember(screenWidthDp, context, selectedAdUnitId) {
        AdView(context).apply {
            adUnitId = selectedAdUnitId
            setAdSize(adSize)
            loadAd(AdRequest.Builder().build())
        }
    }

    DisposableEffect(adView) {
        onDispose {
            adView.destroy()
        }
    }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp),
        factory = { adView },
    )
}
