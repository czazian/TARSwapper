package com.example.tarswapper.data

import java.io.Serializable

data class Order (
    val orderID: String? = null,
    val tradeType: String? = null,
    val status: String? = null,
    val createdAt: String? = null,
    val productID: String? = null,
    val meetUpID: String? = null,
) : Serializable
