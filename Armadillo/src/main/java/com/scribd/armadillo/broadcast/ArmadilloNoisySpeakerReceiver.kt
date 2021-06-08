package com.scribd.armadillo.broadcast

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.util.Log

/**
 * Wraps broadcast receiver for "Becoming Noisy" - handles events when a headset is disconnected
 * while playing. Android's default behavior will route sound to the speaker. We instead prefer
 * to pause.
 *
 * This receiver needs to be started when playback plays and then stopped when playback pauses.
 * */
interface ArmadilloNoisySpeakerReceiver {
    fun registerForNoisyEvent(listener: Listener)
    fun unregisterForNoisyEvent()

    interface Listener {
        fun onBecomingNoisy()
    }
}

class ArmadilloNoisyReceiver(val application: Application)
    : BroadcastReceiver(), ArmadilloNoisySpeakerReceiver {
    lateinit var listener: ArmadilloNoisySpeakerReceiver.Listener

    companion object {
        const val TAG = "NoisyBroadcastReceiver"
        val intentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        var isRegistered = false
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
            Log.v(TAG, "becoming noisy")
            listener.onBecomingNoisy()
        }
    }

    override fun registerForNoisyEvent(listener: ArmadilloNoisySpeakerReceiver.Listener) {
        if(!isRegistered) {
            this.listener = listener
            Log.v(TAG, "registered for listening noisy")
            application.registerReceiver(this, intentFilter)
            isRegistered = true
        }
    }

    override fun unregisterForNoisyEvent() {
        if(isRegistered) {
            Log.v(TAG, "unregistered for listening noisy")
            application.unregisterReceiver(this)
            isRegistered = false
        }
    }
}