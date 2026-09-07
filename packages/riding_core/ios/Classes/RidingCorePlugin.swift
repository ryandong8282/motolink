import AVFAudio
import CoreLocation
import Flutter
import UIKit

public final class RidingCorePlugin: NSObject, FlutterPlugin, CLLocationManagerDelegate {
    private let locationManager = CLLocationManager()
    private let audioSession = AVAudioSession.sharedInstance()

    private var provider = "mock"
    private var channelJoined = false
    private var transmitting = false
    private var rideActive = false

    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(
            name: "com.motolink/riding_core",
            binaryMessenger: registrar.messenger()
        )
        let instance = RidingCorePlugin()
        registrar.addMethodCallDelegate(instance, channel: channel)
    }

    public override init() {
        super.init()
        locationManager.delegate = self
        locationManager.activityType = .automotiveNavigation
        locationManager.desiredAccuracy = kCLLocationAccuracyBestForNavigation
        locationManager.distanceFilter = 5
        locationManager.pausesLocationUpdatesAutomatically = false
    }

    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        switch call.method {
        case "initialize":
            result(nil)
        case "joinChannel":
            joinChannel(call, result: result)
        case "leaveChannel":
            leaveChannel(result: result)
        case "startTransmitting":
            startTransmitting(result: result)
        case "stopTransmitting":
            transmitting = false
            result(nil)
        case "startRide":
            startRide(call, result: result)
        case "stopRide":
            stopRide(result: result)
        case "setScreenAwake":
            setScreenAwake(call, result: result)
        case "getStatus":
            result([
                "platform": "ios",
                "provider": provider,
                "state": stateLabel,
                "channelJoined": channelJoined,
                "transmitting": transmitting,
                "rideActive": rideActive,
            ])
        default:
            result(FlutterMethodNotImplemented)
        }
    }

    private func joinChannel(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        guard
            let arguments = call.arguments as? [String: Any],
            let roomId = arguments["roomId"] as? String,
            !roomId.isEmpty,
            let userId = arguments["userId"] as? String,
            !userId.isEmpty
        else {
            result(FlutterError(
                code: "INVALID_ARGUMENT",
                message: "roomId and userId are required",
                details: nil
            ))
            return
        }

        provider = arguments["provider"] as? String ?? "mock"
        do {
            try configureVoiceSession()
            channelJoined = true
            // The selected vendor RTC SDK joins its audio channel here.
            result(nil)
        } catch {
            result(FlutterError(
                code: "AUDIO_SESSION_ERROR",
                message: error.localizedDescription,
                details: nil
            ))
        }
    }

    private func leaveChannel(result: @escaping FlutterResult) {
        transmitting = false
        channelJoined = false
        do {
            try audioSession.setActive(false, options: .notifyOthersOnDeactivation)
            result(nil)
        } catch {
            result(FlutterError(
                code: "AUDIO_SESSION_ERROR",
                message: error.localizedDescription,
                details: nil
            ))
        }
    }

    private func startTransmitting(result: @escaping FlutterResult) {
        guard channelJoined else {
            result(FlutterError(
                code: "NOT_JOINED",
                message: "Join a channel before transmitting",
                details: nil
            ))
            return
        }
        do {
            try configureVoiceSession()
            transmitting = true
            // The RTC adapter enables local audio publication here.
            result(nil)
        } catch {
            result(FlutterError(
                code: "AUDIO_SESSION_ERROR",
                message: error.localizedDescription,
                details: nil
            ))
        }
    }

    private func startRide(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        guard
            let arguments = call.arguments as? [String: Any],
            let rideId = arguments["rideId"] as? String,
            !rideId.isEmpty,
            let userId = arguments["userId"] as? String,
            !userId.isEmpty
        else {
            result(FlutterError(
                code: "INVALID_ARGUMENT",
                message: "rideId and userId are required",
                details: nil
            ))
            return
        }

        locationManager.requestAlwaysAuthorization()
        locationManager.allowsBackgroundLocationUpdates = true
        locationManager.showsBackgroundLocationIndicator = true
        locationManager.startUpdatingLocation()
        rideActive = true
        result(nil)
    }

    private func stopRide(result: @escaping FlutterResult) {
        locationManager.stopUpdatingLocation()
        locationManager.allowsBackgroundLocationUpdates = false
        rideActive = false
        result(nil)
    }

    private func setScreenAwake(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        let arguments = call.arguments as? [String: Any]
        let awake = arguments?["awake"] as? Bool ?? false
        UIApplication.shared.isIdleTimerDisabled = awake
        result(nil)
    }

    private func configureVoiceSession() throws {
        try audioSession.setCategory(
            .playAndRecord,
            mode: .voiceChat,
            options: [.allowBluetooth, .allowBluetoothA2DP, .defaultToSpeaker]
        )
        try audioSession.setActive(true)
    }

    private var stateLabel: String {
        if transmitting { return "transmitting" }
        if rideActive { return "riding" }
        if channelJoined { return "listening" }
        return "idle"
    }

    public func locationManager(
        _ manager: CLLocationManager,
        didUpdateLocations locations: [CLLocation]
    ) {
        // Production code writes points to a durable local queue and uploads batches.
        // Keeping this native avoids losing the ride when Flutter is suspended.
    }

    public func locationManager(
        _ manager: CLLocationManager,
        didFailWithError error: Error
    ) {
        // Feed a structured diagnostic event to Flutter/telemetry in production.
    }
}
