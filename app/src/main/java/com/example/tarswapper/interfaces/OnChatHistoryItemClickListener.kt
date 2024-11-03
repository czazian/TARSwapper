package com.example.tarswapper.interfaces

interface OnChatHistoryItemClickListener {
    fun onItemClick(messageID: String?, oppositeUserID: String, roomID: String)
}