import UIKit
import GoogleMobileAds

private let attachBannerNotification = Notification.Name("WeddingLedgerAdMobBannerAttach")
private let detachBannerNotification = Notification.Name("WeddingLedgerAdMobBannerDetach")
private let iOSTestBannerAdUnitID = "ca-app-pub-3940256099942544/2435281174"

final class AdMobBannerBridge: NSObject {
    static let shared = AdMobBannerBridge()

    private var isStarted = false
    private weak var currentContainerView: UIView?
    private var currentBannerView: BannerView?

    private override init() {
        super.init()
    }

    func start() {
        guard !isStarted else { return }
        isStarted = true

        NotificationCenter.default.addObserver(
            self,
            selector: #selector(attachBanner(_:)),
            name: attachBannerNotification,
            object: nil
        )
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(detachBanner(_:)),
            name: detachBannerNotification,
            object: nil
        )
    }

    @objc private func attachBanner(_ notification: Notification) {
        guard let containerView = notification.object as? UIView else { return }
        DispatchQueue.main.async { [weak self, weak containerView] in
            guard let containerView else { return }
            self?.installBanner(in: containerView)
        }
    }

    private func installBanner(in containerView: UIView) {
        if currentContainerView === containerView, currentBannerView?.superview === containerView {
            return
        }

        detachCurrentBanner()

        let adSize = AdSizeBanner
        let bannerView = BannerView(adSize: adSize)
        bannerView.translatesAutoresizingMaskIntoConstraints = false
        bannerView.adUnitID = resolvedBannerUnitID()
        bannerView.rootViewController = containerView.nearestViewController
        bannerView.delegate = self

        containerView.addSubview(bannerView)
        NSLayoutConstraint.activate([
            bannerView.centerXAnchor.constraint(equalTo: containerView.centerXAnchor),
            bannerView.centerYAnchor.constraint(equalTo: containerView.centerYAnchor),
            bannerView.widthAnchor.constraint(equalToConstant: adSize.size.width),
            bannerView.heightAnchor.constraint(equalToConstant: adSize.size.height),
        ])

        currentContainerView = containerView
        currentBannerView = bannerView
        bannerView.load(Request())
    }

    @objc private func detachBanner(_ notification: Notification) {
        guard let containerView = notification.object as? UIView else { return }
        if currentContainerView === containerView {
            detachCurrentBanner()
        }
    }

    private func detachCurrentBanner() {
        currentBannerView?.delegate = nil
        currentBannerView?.removeFromSuperview()
        currentBannerView = nil
        currentContainerView = nil
    }
}

extension AdMobBannerBridge: BannerViewDelegate {
    func bannerViewDidReceiveAd(_ bannerView: BannerView) {
        NSLog("[WL_ADS][INFO] ios_banner_loaded")
    }

    func bannerView(_ bannerView: BannerView, didFailToReceiveAdWithError error: Error) {
        NSLog("[WL_ADS][WARN] ios_banner_failed error=%@", error.localizedDescription)
    }
}

private extension UIView {
    var nearestViewController: UIViewController? {
        var responder: UIResponder? = self
        while let currentResponder = responder {
            if let viewController = currentResponder as? UIViewController {
                return viewController
            }
            responder = currentResponder.next
        }
        return nil
    }
}

private func resolvedBannerUnitID() -> String {
#if DEBUG
    return iOSTestBannerAdUnitID
#else
    guard
        let configured = Bundle.main.object(forInfoDictionaryKey: "ADMOB_IOS_BANNER_UNIT_ID") as? String
    else {
        return iOSTestBannerAdUnitID
    }

    let normalized = configured.trimmingCharacters(in: .whitespacesAndNewlines)
    return normalized.isEmpty ? iOSTestBannerAdUnitID : normalized
#endif
}
