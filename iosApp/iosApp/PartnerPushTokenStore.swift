import Foundation

enum PartnerPushTokenStore {
    private static let pushTokenKey = "wl_partner_activity_fcm_token"

    static func save(_ token: String?) {
        let defaults = UserDefaults.standard
        let normalized = token?.trimmingCharacters(in: .whitespacesAndNewlines)
        if let normalized, !normalized.isEmpty {
            defaults.set(normalized, forKey: pushTokenKey)
        } else {
            defaults.removeObject(forKey: pushTokenKey)
        }
    }
}
