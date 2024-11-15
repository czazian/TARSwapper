package com.example.tarswapper.data

import java.io.Serializable

data class SwapRequest (
    var swapRequestID: String? = null,
    var status: String? = null,
    var senderProductID: String? = null,
    var receiverProductID: String? = null,
    var created_at: String? = null,
    var meetUpID: String? = null,
) : Serializable