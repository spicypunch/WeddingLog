import SwiftUI
import KakaoSDKAuth
import ComposeApp

@main
struct iOSApp: App {
    init() {
        KakaoNativeSignInCoordinator.shared.startObserving()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    if AuthApi.isKakaoTalkLoginUrl(url) {
                        _ = AuthController.handleOpenUrl(url: url)
                        return
                    }
                    DeepLinkInteropKt.pushIncomingDeepLink(url: url.absoluteString)
                }
        }
    }
}
