package com.motolink.motolink

import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private val channelName = "motolink/riding_core"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            channelName
        ).setMethodCallHandler { call, result ->
            when (call.method) {
                "joinRoom" -> result.success(null)
                "startTransmit" -> result.success(null)
                "stopTransmit" -> result.success(null)
                "leaveRoom" -> result.success(null)
                else -> result.notImplemented()
            }
        }
    }
}
