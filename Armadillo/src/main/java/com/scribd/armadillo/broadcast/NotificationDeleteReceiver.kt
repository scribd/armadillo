package com.scribd.armadillo.broadcast

import android.app.Application
import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.scribd.armadillo.hasSnowCone

/**
 * Wraps a Broadcast Receiver to allow a Listener to know when a given Notification has been deleted.
 */
internal interface NotificationDeleteReceiver {
    fun register(listener: Listener)
    fun unregister()
    fun setDeleteIntentOnNotification(notification: Notification)

    interface Listener {
        fun onNotificationDeleted()
    }
}

internal class ArmadilloNotificationDeleteReceiver(val application: Application) : NotificationDeleteReceiver, BroadcastReceiver() {

    private var deleteListener: NotificationDeleteReceiver.Listener? = null
    private var isRegistered = false

    companion object {
        const val ACTION = "com.scribd.armadillo.ACTION_NOTIFICATION_DELETED"
    }

    override fun register(listener: NotificationDeleteReceiver.Listener) {
        if (!isRegistered) {
            application.registerReceiver(this, IntentFilter(ACTION))
            deleteListener = listener
            isRegistered = true
        }
    }

    override fun unregister() {
        if (isRegistered) {
            application.unregisterReceiver(this)
            deleteListener = null
            isRegistered = false
        }
    }

    /**
     * Sets a delete Intent on a Notification that will trigger this Broadcast Receiver if the Notification is deleted.
     */
    override fun setDeleteIntentOnNotification(notification: Notification) {
        val intent = Intent(ArmadilloNotificationDeleteReceiver.ACTION)
        val intentFlag = if (hasSnowCone()) PendingIntent.FLAG_MUTABLE else 0
        val pendingIntent = PendingIntent.getBroadcast(application, 0, intent, intentFlag)
        notification.deleteIntent = pendingIntent
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        deleteListener?.onNotificationDeleted()
    }
}

