package com.example.tarswapper.data

import java.io.Serializable

data class Items(
    var itemID: Int? = 0,
    var itemName: String? = "",
    var itemRequireCoin: Int? = 0,
    var itemType: String? = "",
    var itemURL: String? = "",
):Serializable
