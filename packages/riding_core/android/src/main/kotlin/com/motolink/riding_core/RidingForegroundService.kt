package com.motolink.riding_core

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.IBinder

class RidingForegroundService : Service() {
    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var pttActive: Boolean = false
    private var rideActive: Boolean = false

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification("骑行服务正在运行"))
        when (intent?.action) {
            ACTION_START_PTT -> {
                pttActive = true
                requestVoiceAudioFocus()
                updateNotification("正在发送车队语音")
            }
            ACTION_STOP_PTT -> {
                pttActive = false
                abandonVoiceAudioFocus()
                updateNotification(if (rideActive) "正在记录骑行" else "对讲已停止")
                stopIfIdle()
            }
            ACTION_START_RIDE -> {
                rideActive = true
                updateNotification("正在记录骑行与队内位置")
                // A production adapter starts FusedLocationProvider updates here and
                // persists points locally before uploading them in batches.
            }
            ACTION_STOP_RIDE -> {
                rideActive = false
                updateNotification(if (pttActive) "正在发送车队语音" else "骑行已结束")
                stopIfIdle()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        abandonVoiceAudioFocus()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun requestVoiceAudioFocus() {
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(attributes)
            .setAcceptsDelayedFocusGain(false)
            .setOnAudioFocusChangeListener { /* RTC adapter reacts to loss here. */ }
            .build()
        audioFocusRequest = request
        audioManager.requestAudioFocus(request)
    }

    private fun abandonVoiceAudioFocus() {
        audioFocusRequest?.let(audioManager::abandonAudioFocusRequest)
        audioFocusRequest = null
        audioManager.mode = AudioManager.MODE_NORMAL
    }

    private fun stopIfIdle() {
        if (!pttActive && !rideActive) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "MotoLink 骑行服务",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "骑行定位与车队语音运行状态"
        }
        manager.createNotificationChannel(channel)
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun buildNotification(text: String): Notification {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = launchIntent?.let {
            PendingIntent.getActivity(
                this,
                0,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
        return Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_headset)
            .setContentTitle("MotoLink")
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    companion object {
        const val ACTION_START_PTT = "com.motolink.riding_core.START_PTT"
        const val ACTION_STOP_PTT = "com.motolink.riding_core.STOP_PTT"
        const val ACTION_START_RIDE = "com.motolink.riding_core.START_RIDE"
        const val ACTION_STOP_RIDE = "com.motolink.riding_core.STOP_RIDE"

        const val EXTRA_RIDE_ID = "rideId"
        const val EXTRA_ROOM_ID = "roomId"
        const val EXTRA_USER_ID = "userId"

        private const val NOTIFICATION_CHANNEL_ID = "motolink_riding"
        private const val NOTIFICATION_ID = 41001
    }
}
