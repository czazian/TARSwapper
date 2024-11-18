package com.example.tarswapper.data

import java.io.Serializable

data class MeetUp (
    var meetUpID: String? = null,
    val date: String? = null,
    val time: String? = null,
    val location: String? = null,
    val venue: String? = null,
    val verificationCode: Int? = null,   //Added for Identity Verification
    val verifiedStatus: Boolean = false, //Added for Identity Verification
    val completeStatus: Boolean = false, //Added for Identity Verification
) : Serializable
