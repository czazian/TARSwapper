package com.example.tarswapper.data

import java.io.Serializable

data class Community(
    var communityID: String? = null,
    var title: String? = null,
    var description: String? = null,
    var view: Int? = null,
    var status: String? = null,
    var created_at: String? = null,
    var created_by_UserID: String? = null,
): Serializable