package com.adith.smash.x.whatsappclone.listener

interface ChatClickListener {
    fun onChatClicked(
        chatId: String?,
        otherUserId: String?,
        chatsImageUrl: String?,
        chatsName: String?
    )
}