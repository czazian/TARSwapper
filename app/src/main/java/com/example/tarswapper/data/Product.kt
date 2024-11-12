package com.example.tarswapper.data

import java.io.Serializable

data class Product(
    var productID: String? = null,
    var name: String? = null,
    var description: String? = null,
    var category: String? = null,
    var tradeType: String? = null,
    var status: String? = null,
    var created_at: String? = null,
): Serializable
