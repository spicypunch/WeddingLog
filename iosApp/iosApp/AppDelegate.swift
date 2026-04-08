import UIKit
import UserNotifications
import FirebaseCore
import FirebaseMessaging
import GoogleMobileAds
import ComposeApp

final class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate, MessagingDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        configureFirebaseIfPossible()
        configureGoogleMobileAdsIfPossible()
        UNUserNotificationCenter.current().delegate = self
        Messaging.messaging().delegate = self
        application.registerForRemoteNotifications()
        return true
    }

    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        Messaging.messaging().apnsToken = deviceToken
    }

    func application(_ application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: Error) {
        NSLog("[WL_PUSH][WARN] apns_registration_failed error=%@", error.localizedDescription)
    }

    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        PartnerPushTokenStore.save(fcmToken)
        PushTokenHostBridgeKt.syncPartnerPushTokenFromHost()
        NSLog("[WL_PUSH][INFO] fcm_token_updated hasToken=%@", fcmToken == nil ? "false" : "true")
    }

    private func configureFirebaseIfPossible() {
        guard FirebaseApp.app() == nil else { return }
        guard
            let googleAppID = Bundle.main.object(forInfoDictionaryKey: "FIREBASE_IOS_GOOGLE_APP_ID") as? String,
            let projectID = Bundle.main.object(forInfoDictionaryKey: "FIREBASE_PROJECT_ID") as? String,
            let apiKey = Bundle.main.object(forInfoDictionaryKey: "FIREBASE_API_KEY") as? String,
            let senderID = Bundle.main.object(forInfoDictionaryKey: "FIREBASE_GCM_SENDER_ID") as? String
        else {
            NSLog("[WL_PUSH][WARN] firebase_config_missing")
            return
        }

        let normalizedAppID = googleAppID.trimmingCharacters(in: .whitespacesAndNewlines)
        let normalizedProjectID = projectID.trimmingCharacters(in: .whitespacesAndNewlines)
        let normalizedApiKey = apiKey.trimmingCharacters(in: .whitespacesAndNewlines)
        let normalizedSenderID = senderID.trimmingCharacters(in: .whitespacesAndNewlines)

        guard
            !normalizedAppID.isEmpty,
            !normalizedProjectID.isEmpty,
            !normalizedApiKey.isEmpty,
            !normalizedSenderID.isEmpty
        else {
            NSLog("[WL_PUSH][WARN] firebase_config_blank")
            return
        }

        let options = FirebaseOptions(googleAppID: normalizedAppID, gcmSenderID: normalizedSenderID)
        options.projectID = normalizedProjectID
        options.apiKey = normalizedApiKey
        options.bundleID = Bundle.main.bundleIdentifier ?? ""
        FirebaseApp.configure(options: options)
    }

    private func configureGoogleMobileAdsIfPossible() {
        guard
            let appID = Bundle.main.object(forInfoDictionaryKey: "GADApplicationIdentifier") as? String
        else {
            NSLog("[WL_ADS][WARN] admob_app_id_missing")
            return
        }

        let normalizedAppID = appID.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !normalizedAppID.isEmpty else {
            NSLog("[WL_ADS][WARN] admob_app_id_blank")
            return
        }

        MobileAds.shared.start()
        NSLog("[WL_ADS][INFO] admob_initialized")
    }
}
