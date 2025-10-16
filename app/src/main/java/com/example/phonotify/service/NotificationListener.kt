package com.example.phonotify.service

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Bundle
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.phonotify.ViewModelData
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class NotificationListener: NotificationListenerService() {
    private val TAG = "notListener"
    private var prevTitle = "null"
    private var prevContext = "null"
    private var prevPackage = "null"
    @Override
    override fun onBind(intent: Intent?): IBinder? {
        return super.onBind(intent)
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG,"Created NotificationListener")
    }
    @Override
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if(sbn == null) return
        val extras: Bundle = sbn.notification.extras

        var title = extras.getString("android.title") ?: "No Title"
        var text = extras.getCharSequence("android.text")?.toString() ?: "No Text"

        if(sbn.packageName == "com.spotify.music" || sbn.packageName == "com.google.youtube-music.com"){
           val arr = getActiveMediaInfo(this)
            title = arr[0]
            text = arr[1]
        }
        val nData = NotificationData(title,text,sbn.packageName)

        if(prevTitle != title || prevContext != text || prevPackage != sbn.packageName) {
            GlobalScope.launch {
               Log.d(TAG,"Emitting... \n current: ${title} ${text} ${sbn.packageName} \n previous: ${prevTitle} ${prevContext} ${prevPackage} ")
                ViewModelData.notyData.emit(nData)
            }
        }
        prevTitle = title
        prevContext = text
        prevPackage = sbn.packageName
        Log.d(TAG,"Notification Posted... ${prevTitle} ${prevContext} ${prevPackage}")
    }

    @Override
    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        Log.d(TAG,"Notification Removed...")
        super.onNotificationRemoved(sbn)
    }

    override fun onDestroy() {
        Log.d(TAG,"Destroyed NotificationListener")
        super.onDestroy()
    }

    @SuppressLint("MissingPermission")
    fun getActiveMediaInfo(context: Context): Array<String> {
        var title = "None"
        var artist = "None"
        try {
            val msm = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            val controllers = msm.getActiveSessions(ComponentName(this, NotificationListener::class.java))

            for (controller in controllers) {
                val pkg = controller.packageName
                if (pkg == "com.spotify.music" || pkg == "com.google.android.apps.youtube.music") {
                    val metadata = controller.metadata
                    val playbackState = controller.playbackState

                    title = metadata?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE) ?: "None"
                    artist = metadata?.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST) ?: "None"
                    val album = metadata?.getString(android.media.MediaMetadata.METADATA_KEY_ALBUM)

                    val playing = playbackState?.state == PlaybackState.STATE_PLAYING

                    Log.d(TAG, "[$pkg] $title — $artist | $album | Playing=$playing")
                }
            }
        }
        catch (e: Exception){
            Log.e(TAG," crashed ActiveMediaInfo $e")
        }
        return arrayOf(title,artist)
    }
}