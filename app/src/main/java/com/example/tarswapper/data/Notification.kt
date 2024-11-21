package com.example.tarswapper.data

import java.io.Serializable

data class Notification(
    var notificationID: String? = null,
    var notificationType: String? = null,
    var notificationDateTime: String? = null,
    var notification: String? =null,
    var userID: String? = null,
) : Serializable
