package com.example.tarswapper.data
class Message {
    //Initialization
    var messageID: String? = null
    var message: String? = null
    var senderID: String? = null
    var media: String? = null
    var dateTime: String? = null
    var status: Boolean? = false
    var translatedText: String? = null
    var mediaType: String? = null

    constructor(){}
    constructor(
        message: String?,
        senderID: String?,
        dateTime: String?,
        media: String?,
        mediaType: String?
    ) {
        this.message = message
        this.senderID = senderID
        this.dateTime = dateTime
        this.media = media
        this.mediaType = mediaType
    }
}

