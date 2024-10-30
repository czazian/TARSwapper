package com.example.tarswapper.data

import java.io.Serializable

data class PurchasedItem (
    var userID: String? = "",
    var itemID: Int? = 0,
    var isEquipped: Boolean? = false,
):Serializable
