package com.example.tarswapper.data

import java.io.Serializable

data class ShortVideo(
    var shortVideoID: String? = null,
    var title: String? = null,
    var created_at: String? = null,
    var created_by_UserID: String? = null,
): Serializable
