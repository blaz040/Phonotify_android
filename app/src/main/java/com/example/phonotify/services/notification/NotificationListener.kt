package com.example.phonotify.services.notification

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.phonotify.ViewModelData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber


class NotificationListener: NotificationListenerService() {
    private val TAG = "notListener"
    private var prevTitle = "null"
    private var prevContext = "null"
    private var prevPackage = "null"

    private val onGoingNotifications = NotificationData.onGoingNotifications
    private val allowedPackages = NotificationData.allowedPackages
    private val localScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)


    @Override
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        if(sbn == null) return

        val extras: Bundle = sbn.notification.extras
        val title = extras.getString("android.title") ?: "No Title"
        val text = extras.getCharSequence("android.text") ?.toString() ?: "No Text"
        val importance = sbn.notification.priority
        val channelID = sbn.notification.channelId
        val notificationKey = sbn.key

        if (importance < 0) return

        val nData = Notification(title, text, sbn.packageName)
        var ignore = false
        try {
            if (sbn.packageName !in allowedPackages) ignore = true
            if (onGoingNotifications[notificationKey] != null) ignore = true // duplicate notification
        }
        finally{
            if (ignore)
                if ( sbn.packageName in allowedPackages){
                    Timber.w("Ignoring from${sbn.packageName}: txt: ${text}")
                }
        }

        onGoingNotifications[notificationKey] = nData

        localScope.launch {
            ViewModelData.notyData.emit(nData)
        }
        Timber.d("Notification Posted ${sbn.packageName} ${notificationKey}")

//        prevTitle = title
//        prevContext = text
//        prevPackage = sbn.packageName

        //Timber.d("Notification Posted... ${prevTitle} ${prevContext} ${prevPackage} ${channelID}")
    }

    @Override
    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if(sbn == null) return
        val notificationKey = sbn.key

        if (onGoingNotifications[notificationKey] != null) {
            Timber.e("Removed notification that is not in MAP $notificationKey")
            return
        }
        onGoingNotifications.remove(notificationKey)
        Timber.d("Removed $notificationKey")
    }

//    @SuppressLint("MissingPermission")
//    fun getActiveMediaInfo(context: Context): Array<String> {
//        var title = "None"
//        var artist = "None"
//        try {
//            val msm = context.getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
//            val controllers = msm.getActiveSessions(ComponentName(this, NotificationListener::class.java))
//
//            for (controller in controllers) {
//                val pkg = controller.packageName
//                if (pkg == "com.spotify.music" || pkg == "com.google.android.apps.youtube.music") {
//                    val metadata = controller.metadata
//                    val playbackState = controller.playbackState
//
//                    title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "None"
//                    artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "None"
//                    val album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM)
//
//                    val playing = playbackState?.state == PlaybackState.STATE_PLAYING
//                }
//            }
//        }
//        catch (e: Exception){
//            Timber.e("crashed ActiveMediaInfo $e")
//        }
//        return arrayOf(title,artist)
//    }
}