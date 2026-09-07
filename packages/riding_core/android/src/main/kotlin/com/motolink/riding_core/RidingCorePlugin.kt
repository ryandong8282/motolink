package com.motolink.riding_core

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.WindowManager
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

class RidingCorePlugin : FlutterPlugin, MethodChannel.MethodCallHandler, ActivityAware {
    private lateinit var applicationContext: Context
    private lateinit var channel: MethodChannel
    private var activity: Activity? = null

    private var provider: String = "mock"
    private var channelJoined: Boolean = false
    private var transmitting: Boolean = false
    private var rideActive: Boolean = false

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        applicationContext = binding.applicationContext
        channel = MethodChannel(binding.binaryMessenger, CHANNEL_NAME)
        channel.setMethodCallHandler(this)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "initialize" -> result.success(null)
            "joinChannel" -> joinChannel(call, result)
            "leaveChannel" -> {
                channelJoined = false
                transmitting = false
                dispatch(RidingForegroundService.ACTION_STOP_PTT)
                result.success(null)
            }
            "startTransmitting" -> startTransmitting(result)
            "stopTransmitting" -> {
                transmitting = false
                dispatch(RidingForegroundService.ACTION_STOP_PTT)
                result.success(null)
            }
            "startRide" -> startRide(call, result)
            "stopRide" -> {
                rideActive = false
                dispatch(RidingForegroundService.ACTION_STOP_RIDE)
                result.success(null)
            }
            "setScreenAwake" -> setScreenAwake(call, result)
            "getStatus" -> result.success(
                mapOf(
                    "platform" to "android",
                    "provider" to provider,
                    "state" to stateLabel(),
                    "channelJoined" to channelJoined,
                    "transmitting" to transmitting,
                    "rideActive" to rideActive,
                )
            )
            else -> result.notImplemented()
        }
    }

    private fun joinChannel(call: MethodCall, result: MethodChannel.Result) {
        val roomId = call.argument<String>("roomId")
        val userId = call.argument<String>("userId")
        provider = call.argument<String>("provider") ?: "mock"
        if (roomId.isNullOrBlank() || userId.isNullOrBlank()) {
            result.error("INVALID_ARGUMENT", "roomId and userId are required", null)
            return
        }

        // Vendor RTC join belongs here. The scaffold intentionally keeps media mocked.
        channelJoined = true
        result.success(null)
    }

    private fun startTransmitting(result: MethodChannel.Result) {
        if (!channelJoined) {
            result.error("NOT_JOINED", "Join a channel before transmitting", null)
            return
        }
        transmitting = true
        dispatch(RidingForegroundService.ACTION_START_PTT)
        result.success(null)
    }

    private fun startRide(call: MethodCall, result: MethodChannel.Result) {
        val rideId = call.argument<String>("rideId")
        val userId = call.argument<String>("userId")
        if (rideId.isNullOrBlank() || userId.isNullOrBlank()) {
            result.error("INVALID_ARGUMENT", "rideId and userId are required", null)
            return
        }
        rideActive = true
        dispatch(
            RidingForegroundService.ACTION_START_RIDE,
            mapOf(
                RidingForegroundService.EXTRA_RIDE_ID to rideId,
                RidingForegroundService.EXTRA_ROOM_ID to call.argument<String>("roomId"),
                RidingForegroundService.EXTRA_USER_ID to userId,
            )
        )
        result.success(null)
    }

    private fun setScreenAwake(call: MethodCall, result: MethodChannel.Result) {
        val awake = call.argument<Boolean>("awake") ?: false
        val currentActivity = activity
        if (currentActivity == null) {
            result.error("NO_ACTIVITY", "No foreground activity is attached", null)
            return
        }
        currentActivity.runOnUiThread {
            if (awake) {
                currentActivity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                currentActivity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            result.success(null)
        }
    }

    private fun dispatch(action: String, extras: Map<String, String?> = emptyMap()) {
        val intent = Intent(applicationContext, RidingForegroundService::class.java)
            .setAction(action)
        extras.forEach { (key, value) ->
            if (value != null) {
                intent.putExtra(key, value)
            }
        }
        applicationContext.startForegroundService(intent)
    }

    private fun stateLabel(): String = when {
        transmitting -> "transmitting"
        rideActive -> "riding"
        channelJoined -> "listening"
        else -> "idle"
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivityForConfigChanges() {
        activity = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivity() {
        activity = null
    }

    companion object {
        private const val CHANNEL_NAME = "com.motolink/riding_core"
    }
}
