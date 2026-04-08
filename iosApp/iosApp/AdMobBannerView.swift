import SwiftUI
import UIKit
import GoogleMobileAds

private let iOSTestBannerAdUnitID = "ca-app-pub-3940256099942544/2435281174"

struct AdMobBannerContainerView: View {
    private var bannerWidth: CGFloat {
        UIScreen.main.bounds.width
    }

    private var adSize: AdSize {
        largeAnchoredAdaptiveBanner(width: bannerWidth)
    }

    var body: some View {
        BannerViewContainer(adSize: adSize)
            .frame(width: adSize.size.width, height: adSize.size.height)
            .frame(maxWidth: .infinity)
            .background(Color(.systemBackground))
    }
}

private struct BannerViewContainer: UIViewRepresentable {
    typealias UIViewType = BannerView

    let adSize: AdSize

    func makeCoordinator() -> Coordinator {
        Coordinator()
    }

    func makeUIView(context: Context) -> BannerView {
        let banner = BannerView(adSize: adSize)
        banner.adUnitID = resolvedBannerUnitID()
        banner.delegate = context.coordinator
        banner.load(Request())
        return banner
    }

    func updateUIView(_ uiView: BannerView, context: Context) {
        if uiView.adSize.size != adSize.size {
            uiView.adSize = adSize
            uiView.load(Request())
        }
    }

    final class Coordinator: NSObject, BannerViewDelegate {
        func bannerViewDidReceiveAd(_ bannerView: BannerView) {
            NSLog("[WL_ADS][INFO] ios_banner_loaded")
        }

        func bannerView(_ bannerView: BannerView, didFailToReceiveAdWithError error: Error) {
            NSLog("[WL_ADS][WARN] ios_banner_failed error=%@", error.localizedDescription)
        }
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
