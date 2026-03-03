package kr.jm.weddingledger

import android.app.Application
import com.kakao.sdk.common.KakaoSdk
import kr.jm.weddingledger.core.config.BuildConfig
import kr.jm.weddingledger.core.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class WeddingLedgerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.KAKAO_NATIVE_APP_KEY.isNotBlank()) {
            KakaoSdk.init(context = this, appKey = BuildConfig.KAKAO_NATIVE_APP_KEY)
        }
        startKoin {
            androidLogger()
            androidContext(this@WeddingLedgerApp)
            modules(appModules())
        }
    }
}
