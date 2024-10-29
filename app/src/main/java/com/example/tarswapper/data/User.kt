package com.example.tarswapper.data

import java.io.Serializable
import java.time.LocalDate
import java.util.Date

data class User(
    var userID: String? = "",
    var name: String? = "",
    var email:String? = "",
    var password: String? = "",
    var profileImage: String? = "",
    var joinedDate: String? = null,
    var coinAmount: Int? = 0,
    var isActive: Boolean? = false,
    var gameChance: Boolean? = false,
    var lastPlayDate: String? = "",
):Serializable
