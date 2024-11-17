package com.example.tarswapper.data

import java.io.Serializable

data class CommunityComment(
    var comment: String? = null,
    var userID: String? = null,
): Serializable
