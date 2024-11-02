package com.example.tarswapper.data

import java.io.Serializable

data class ChatRoom(
    var roomID: String? = null,
    val oppositeUserID: String? = null,
    var lastMsg: String? = null,
    val lastMsgTime: String? = null,
):Serializable
