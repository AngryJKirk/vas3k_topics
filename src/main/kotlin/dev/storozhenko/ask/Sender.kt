package dev.storozhenko.ask

import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.bots.AbsSender

class Sender(
    private val chats: Map<Topic, List<String>>,
    private val channelId: String
) {

    fun broadcast(question: Question, absSender: AbsSender) {
        val channelMessage = SendMessage.builder()
            .chatId(channelId)
            .text(question.toString())
            .build()
        absSender.execute(channelMessage)
        val chatIds = chats[question.topic] ?: return
        chatIds.forEach {
            val message = SendMessage.builder()
                .chatId(it)
                .text(question.toString())
                .build()
            absSender.execute(message)
        }
    }
}