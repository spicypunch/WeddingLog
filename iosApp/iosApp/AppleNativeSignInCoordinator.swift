import AuthenticationServices
import ComposeApp
import CryptoKit
import Foundation
import UIKit

final class AppleNativeSignInCoordinator: NSObject {
    static let shared = AppleNativeSignInCoordinator()

    private let signInRequestNotification = Notification.Name("WeddingLedgerAppleNativeSignInRequest")
    private var observer: NSObjectProtocol?
    private var isInProgress = false
    private var currentNonce: String?

    private override init() {
        super.init()
    }

    func startObserving() {
        guard observer == nil else { return }

        observer = NotificationCenter.default.addObserver(
            forName: signInRequestNotification,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            self?.startAppleSignIn()
        }
    }

    deinit {
        if let observer {
            NotificationCenter.default.removeObserver(observer)
        }
    }

    private func startAppleSignIn() {
        guard !isInProgress else {
            finish(idToken: nil, rawNonce: nil, errorMessage: "Apple 로그인이 이미 진행 중입니다.")
            return
        }

        isInProgress = true
        let nonce = makeNonce()
        currentNonce = nonce

        let request = ASAuthorizationAppleIDProvider().createRequest()
        request.requestedScopes = [.fullName, .email]
        request.nonce = sha256(nonce)

        let controller = ASAuthorizationController(authorizationRequests: [request])
        controller.delegate = self
        controller.presentationContextProvider = self
        controller.performRequests()
    }

    private func finish(idToken: String?, rawNonce: String?, errorMessage: String?) {
        isInProgress = false
        currentNonce = nil
        DispatchQueue.main.async {
            IosAppleNativeSignInBridgeKt.completeIosAppleNativeSignIn(
                idToken: idToken,
                rawNonce: rawNonce,
                errorMessage: errorMessage
            )
        }
    }

    private func makeNonce(length: Int = 32) -> String {
        let charset = Array("0123456789ABCDEFGHIJKLMNOPQRSTUVXYZabcdefghijklmnopqrstuvwxyz-._")
        var result = ""
        var remaining = length

        while remaining > 0 {
            var bytes = [UInt8](repeating: 0, count: 16)
            let status = SecRandomCopyBytes(kSecRandomDefault, bytes.count, &bytes)
            if status != errSecSuccess {
                return UUID().uuidString.replacingOccurrences(of: "-", with: "")
            }

            bytes.forEach { byte in
                if remaining == 0 { return }
                if byte < charset.count {
                    result.append(charset[Int(byte)])
                    remaining -= 1
                }
            }
        }

        return result
    }

    private func sha256(_ input: String) -> String {
        let inputData = Data(input.utf8)
        let hashedData = SHA256.hash(data: inputData)
        return hashedData.map { String(format: "%02x", $0) }.joined()
    }

    private func userMessage(for error: Error) -> String {
        guard let appleError = error as? ASAuthorizationError else {
            return error.localizedDescription
        }

        switch appleError.code {
        case .canceled:
            return "Apple 로그인을 취소했습니다."
        case .failed:
            return "Apple 로그인을 완료하지 못했습니다. 잠시 후 다시 시도해주세요."
        case .invalidResponse:
            return "Apple 로그인 응답이 올바르지 않습니다."
        case .notHandled:
            return "Apple 로그인 요청을 처리하지 못했습니다."
        case .unknown:
            fallthrough
        @unknown default:
            return "Apple 로그인을 완료하지 못했습니다."
        }
    }
}

extension AppleNativeSignInCoordinator: ASAuthorizationControllerDelegate {
    func authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithAuthorization authorization: ASAuthorization
    ) {
        guard let credential = authorization.credential as? ASAuthorizationAppleIDCredential else {
            finish(idToken: nil, rawNonce: nil, errorMessage: "Apple 로그인 자격 증명을 확인하지 못했습니다.")
            return
        }

        guard
            let identityTokenData = credential.identityToken,
            let identityToken = String(data: identityTokenData, encoding: .utf8),
            !identityToken.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
        else {
            finish(idToken: nil, rawNonce: nil, errorMessage: "Apple ID 토큰이 비어 있습니다.")
            return
        }

        guard let nonce = currentNonce, !nonce.isEmpty else {
            finish(idToken: nil, rawNonce: nil, errorMessage: "Apple 로그인 nonce를 확인하지 못했습니다.")
            return
        }

        finish(idToken: identityToken, rawNonce: nonce, errorMessage: nil)
    }

    func authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithError error: Error
    ) {
        finish(idToken: nil, rawNonce: nil, errorMessage: userMessage(for: error))
    }
}

extension AppleNativeSignInCoordinator: ASAuthorizationControllerPresentationContextProviding {
    func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
        let scenes = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
        for scene in scenes {
            if let window = scene.windows.first(where: \.isKeyWindow) {
                return window
            }
        }
        return scenes.first?.windows.first ?? ASPresentationAnchor()
    }
}
