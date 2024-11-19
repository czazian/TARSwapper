package com.example.tarswapper.data

import java.io.Serializable

data class Transaction(
    var transactionID: String? = null,
    var scheduledDateTime: String? = null,
    var completedDateTime: String? = null,
    var destination: String? = null,
    var totalDuration: String? = null,
    var giverID: String? = null,
    var takerID: String? = null,
):Serializable
