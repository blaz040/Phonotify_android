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


        /*
        if(sbn.packageName == "com.spotify.music" || sbn.packageName == "com.google.youtube-music.com"){

            val arr = getActiveMediaInfo(this)
            title = arr[0]
            text = arr[1]
        }
        else{
            if(importance < 0) return
        }
        */
        if (importance < 0) return

        //if (NotificationData.notifications )
        /*
        if(sbn.isOngoing) {
            val key = "|$channelID $title"

            if(onGoingNotifications.contains(key)){
                Timber.d("Ignored $key")
                return
            }
            else {
                onGoingNotifications.removeIf { str->
                    if( str.contains(Regex(channelID)) ){
                        return@removeIf true
                    }
                    false
                }
                onGoingNotifications.add(key)
                Timber.d("${onGoingNotifications.toString()}")
                Timber.d("Added $key")
            }
        }
        */
        val nData = Notification(title, text, sbn.packageName)
        if (sbn.packageName !in allowedPackages) return
        if (onGoingNotifications[notificationKey] != null ) return // duplicate notification

        onGoingNotifications[notificationKey] = nData

        //if(prevTitle != title || prevContext != text || prevPackage != sbn.packageName) {
        //}
        localScope.launch {
            ViewModelData.notyData.emit(nData)
        }
        Timber.d("Notification Posted $notificationKey $channelID")

        prevTitle = title
        prevContext = text
        prevPackage = sbn.packageName

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
        /*
        if(sbn.isOngoing ){
            val extras: Bundle = sbn.notification.extras
            var title = extras.getString("android.title") ?: "No Title"
            var text = extras.getCharSequence("android.text")?.toString() ?: "No Text"
            val channelID = sbn.notification.channelId

            if(sbn.packageName == "com.spotify.music" || sbn.packageName == "com.google.youtube-music.com"){
                if(onGoingNotifications.contains(channelID)) Timber.e("This onGoing notification already contains $channelID")
                onGoingNotifications.add(channelID)
                val arr = getActiveMediaInfo(this)
                title = arr[0]
                text = arr[1]
            }
            val key = "$title $text $channelID"
            Timber.d("Removed $key")

            if(onGoingNotifications.contains(key))
                onGoingNotifications.remove(key)
        }
        */
    }

    @SuppressLint("MissingPermission")
    fun getActiveMediaInfo(context: Context): Array<String> {
        var title = "None"
        var artist = "None"
        try {
            val msm = context.getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
            val controllers = msm.getActiveSessions(ComponentName(this, NotificationListener::class.java))

            for (controller in controllers) {
                val pkg = controller.packageName
                if (pkg == "com.spotify.music" || pkg == "com.google.android.apps.youtube.music") {
                    val metadata = controller.metadata
                    val playbackState = controller.playbackState

                    title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "None"
                    artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "None"
                    val album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM)

                    val playing = playbackState?.state == PlaybackState.STATE_PLAYING
                }
            }
        }
        catch (e: Exception){
            Timber.e("crashed ActiveMediaInfo $e")
        }
        return arrayOf(title,artist)
    }
}