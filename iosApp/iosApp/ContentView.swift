import UIKit
import SwiftUI
import ComposeApp

private let showHostBannerNotification = Notification.Name("WeddingLedgerShowHostBanner")
private let hideHostBannerNotification = Notification.Name("WeddingLedgerHideHostBanner")

struct ComposeView: UIViewControllerRepresentable {
    func makeCoordinator() -> Coordinator {
        Coordinator()
    }

    func makeUIViewController(context: Context) -> UIViewController {
        let viewController = MainViewControllerKt.MainViewController()
        let tapGesture = UITapGestureRecognizer(
            target: context.coordinator,
            action: #selector(Coordinator.dismissKeyboard(_:))
        )
        tapGesture.cancelsTouchesInView = false
        tapGesture.delegate = context.coordinator
        viewController.view.addGestureRecognizer(tapGesture)
        return viewController
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}

    final class Coordinator: NSObject, UIGestureRecognizerDelegate {
        private var isKeyboardVisible = false

        override init() {
            super.init()

            NotificationCenter.default.addObserver(
                self,
                selector: #selector(keyboardWillShow),
                name: UIResponder.keyboardWillShowNotification,
                object: nil
            )
            NotificationCenter.default.addObserver(
                self,
                selector: #selector(keyboardWillHide),
                name: UIResponder.keyboardWillHideNotification,
                object: nil
            )
        }

        deinit {
            NotificationCenter.default.removeObserver(self)
        }

        @objc private func keyboardWillShow() {
            isKeyboardVisible = true
        }

        @objc private func keyboardWillHide() {
            isKeyboardVisible = false
        }

        func gestureRecognizer(_ gestureRecognizer: UIGestureRecognizer, shouldReceive touch: UITouch) -> Bool {
            // First tap on a text field should not be intercepted by the global dismiss gesture.
            isKeyboardVisible
        }

        @objc func dismissKeyboard(_ recognizer: UITapGestureRecognizer) {
            recognizer.view?.endEditing(true)
        }
    }
}

struct ContentView: View {
    @State private var isBannerVisible = false

    var body: some View {
        VStack(spacing: 0) {
            ComposeView()
                .frame(maxWidth: .infinity, maxHeight: .infinity)

            if isBannerVisible {
                AdMobBannerContainerView()
                    .transition(.move(edge: .bottom).combined(with: .opacity))
            }
        }
        .ignoresSafeArea(.keyboard, edges: .bottom)
        .onReceive(NotificationCenter.default.publisher(for: showHostBannerNotification)) { _ in
            withAnimation(.easeInOut(duration: 0.2)) {
                isBannerVisible = true
            }
        }
        .onReceive(NotificationCenter.default.publisher(for: hideHostBannerNotification)) { _ in
            withAnimation(.easeInOut(duration: 0.2)) {
                isBannerVisible = false
            }
        }
    }
}
