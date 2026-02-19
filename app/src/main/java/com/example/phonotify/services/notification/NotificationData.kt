package com.example.phonotify.services.notification

object NotificationData {

    val onGoingNotifications = mutableMapOf<String,Notification>()

    val allowedPackages = mutableSetOf<String>()

    fun clearNotifications(){
            onGoingNotifications.clear()
    }
}