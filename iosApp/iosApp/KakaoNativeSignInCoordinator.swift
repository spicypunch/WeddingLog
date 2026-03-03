import Foundation
import KakaoSDKAuth
import KakaoSDKCommon
import KakaoSDKUser
import ComposeApp

final class KakaoNativeSignInCoordinator {
    static let shared = KakaoNativeSignInCoordinator()

    private let signInRequestNotification = Notification.Name("WeddingLedgerKakaoNativeSignInRequest")
    private var observer: NSObjectProtocol?
    private var isInProgress = false
    private var isKakaoSdkInitialized = false

    private init() {
    }

    func startObserving() {
        guard observer == nil else { return }

        observer = NotificationCenter.default.addObserver(
            forName: signInRequestNotification,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            self?.startKakaoSignIn()
        }
    }

    deinit {
        if let observer {
            NotificationCenter.default.removeObserver(observer)
        }
    }

    private func startKakaoSignIn() {
        guard !isInProgress else {
            finish(idToken: nil, accessToken: nil, errorMessage: "카카오 로그인이 이미 진행 중입니다.")
            return
        }

        guard initializeKakaoSdkIfNeeded() else {
            return
        }

        isInProgress = true

        if UserApi.isKakaoTalkLoginAvailable() {
            UserApi.shared.loginWithKakaoTalk { [weak self] token, error in
                guard let self else { return }

                if let error {
                    if self.isUserCancelled(error) {
                        self.handleLoginResult(token: nil, error: error)
                        return
                    }
                    // Talk 앱 로그인 실패 시 계정 로그인으로 1회 폴백합니다.
                    self.loginWithKakaoAccount()
                    return
                }

                self.handleLoginResult(token: token, error: error)
            }
            return
        }

        loginWithKakaoAccount()
    }

    private func loginWithKakaoAccount() {
        UserApi.shared.loginWithKakaoAccount { [weak self] token, error in
            self?.handleLoginResult(token: token, error: error)
        }
    }

    private func handleLoginResult(token: OAuthToken?, error: Error?) {
        if let error {
            finish(idToken: nil, accessToken: nil, errorMessage: toUserMessage(error))
            return
        }

        guard let token else {
            finish(idToken: nil, accessToken: nil, errorMessage: "카카오 로그인 응답이 비어 있습니다.")
            return
        }

        guard let idToken = token.idToken?.trimmingCharacters(in: .whitespacesAndNewlines),
              !idToken.isEmpty else {
            finish(
                idToken: nil,
                accessToken: nil,
                errorMessage: "카카오 ID 토큰이 없습니다. Kakao OpenID Connect 설정을 확인해주세요."
            )
            return
        }

        finish(
            idToken: idToken,
            accessToken: token.accessToken,
            errorMessage: nil
        )
    }

    private func initializeKakaoSdkIfNeeded() -> Bool {
        if isKakaoSdkInitialized {
            return true
        }

        let nativeAppKey = infoValue("KAKAO_NATIVE_APP_KEY")
        if nativeAppKey.isEmpty {
            finish(idToken: nil, accessToken: nil, errorMessage: "KAKAO_NATIVE_APP_KEY가 비어 있습니다.")
            return false
        }

        KakaoSDK.initSDK(appKey: nativeAppKey)
        isKakaoSdkInitialized = true
        return true
    }

    private func finish(idToken: String?, accessToken: String?, errorMessage: String?) {
        isInProgress = false
        DispatchQueue.main.async {
            IosKakaoNativeSignInBridgeKt.completeIosKakaoNativeSignIn(
                idToken: idToken,
                accessToken: accessToken,
                errorMessage: errorMessage
            )
        }
    }

    private func toUserMessage(_ error: Error) -> String {
        let fallback = nonBlank(error.localizedDescription)
            ?? "카카오 로그인을 완료하지 못했습니다. 잠시 후 다시 시도해주세요."

        guard let sdkError = error as? SdkError else {
            return fallback
        }

        if sdkError.isAuthFailed {
            let auth = sdkError.getAuthError()
            let detail = nonBlank(auth.info?.errorDescription)
            switch auth.reason {
            case .AccessDenied:
                return "카카오 로그인을 취소했습니다."
            case .InvalidClient, .Misconfigured:
                let bundleId = Bundle.main.bundleIdentifier ?? "unknown"
                if let detail {
                    return "카카오 앱 설정이 올바르지 않습니다. Kakao Developers의 iOS 번들 ID/네이티브 앱 키를 확인해주세요. (bundleId=\(bundleId), \(auth.reason.rawValue): \(detail))"
                }
                return "카카오 앱 설정이 올바르지 않습니다. Kakao Developers의 iOS 번들 ID/네이티브 앱 키를 확인해주세요. (bundleId=\(bundleId), \(auth.reason.rawValue))"
            default:
                if let detail {
                    return "카카오 인증에 실패했습니다. (\(auth.reason.rawValue): \(detail))"
                }
                return "카카오 인증에 실패했습니다. (\(auth.reason.rawValue))"
            }
        }

        if sdkError.isClientFailed {
            let client = sdkError.getClientError()
            switch client.reason {
            case .Cancelled:
                return "카카오 로그인을 취소했습니다."
            case .MustInitAppKey:
                return "카카오 SDK 초기화에 실패했습니다. KAKAO_NATIVE_APP_KEY를 확인해주세요."
            case .BadParameter:
                return "카카오 로그인 설정값이 올바르지 않습니다. 앱 키와 번들 ID를 확인해주세요."
            case .NotSupported:
                return "현재 환경에서는 카카오톡 앱 로그인을 사용할 수 없습니다. 카카오 계정 로그인을 시도해주세요."
            default:
                return nonBlank(client.message) ?? fallback
            }
        }

        if sdkError.isApiFailed {
            let api = sdkError.getApiError()
            let detail = nonBlank(api.info?.msg)
            if let detail {
                return "카카오 API 호출에 실패했습니다. (\(api.reason.rawValue): \(detail))"
            }
            return "카카오 API 호출에 실패했습니다. (\(api.reason.rawValue))"
        }

        if sdkError.isAppsFailed {
            let apps = sdkError.getAppsError()
            let detail = nonBlank(apps.info?.errorMsg)
            if let detail {
                return "카카오 앱 설정을 확인해주세요. (\(apps.reason.rawValue): \(detail))"
            }
            return "카카오 앱 설정을 확인해주세요. (\(apps.reason.rawValue))"
        }

        return fallback
    }

    private func isUserCancelled(_ error: Error) -> Bool {
        guard let sdkError = error as? SdkError else { return false }
        if sdkError.isClientFailed {
            return sdkError.getClientError().reason == .Cancelled
        }
        if sdkError.isAuthFailed {
            return sdkError.getAuthError().reason == .AccessDenied
        }
        return false
    }

    private func nonBlank(_ value: String?) -> String? {
        guard let value else { return nil }
        let trimmed = value.trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? nil : trimmed
    }

    private func infoValue(_ key: String) -> String {
        (Bundle.main.object(forInfoDictionaryKey: key) as? String)?
            .trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
    }
}
