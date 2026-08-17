import Flutter
import GoogleMaps
import UIKit

@main
@objc class AppDelegate: FlutterAppDelegate, FlutterImplicitEngineDelegate {
  override func application(
    _ application: UIApplication,
    didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
  ) -> Bool {
    // UIScene lifecycle: do NOT register plugins here.
    // Keep only native setup that must run before the Flutter engine starts.
    provideGoogleMapsApiKeyIfAvailable()
    return super.application(application, didFinishLaunchingWithOptions: launchOptions)
  }

  func didInitializeImplicitFlutterEngine(_ engineBridge: FlutterImplicitEngineBridge) {
    GeneratedPluginRegistrant.register(with: engineBridge.pluginRegistry)
  }

  private func provideGoogleMapsApiKeyIfAvailable() {
    guard let apiKey = Bundle.main.object(forInfoDictionaryKey: "GMSApiKey") as? String else {
      return
    }
    let trimmed = apiKey.trimmingCharacters(in: .whitespacesAndNewlines)
    // Skip unresolved xcconfig placeholders and example placeholders.
    guard !trimmed.isEmpty,
          !trimmed.hasPrefix("$("),
          !trimmed.hasPrefix("YOUR_"),
          trimmed.hasPrefix("AIza")
    else {
      return
    }
    GMSServices.provideAPIKey(trimmed)
  }
}
