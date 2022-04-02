package dev.storozhenko.ask.services

import dev.storozhenko.ask.getLogger
import dev.storozhenko.ask.models.Question
import dev.storozhenko.ask.models.Topic
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.bots.AbsSender

class Sender(
    private val chats: Map<Topic, List<String>>,
    private val channelId: String
) {
    private val log = getLogger()

    fun broadcast(question: Question, absSender: AbsSender) {
        val channelMessage = SendMessage.builder()
            .chatId(channelId)
            .text(question.toString())
            .build()
        absSender.execute(channelMessage)
        if (question.topic == Topic.OTHER) {
            return
        }

        val chatIds = chats[question.topic] ?: return
        chatIds.forEach {
            runCatching {
                val message = SendMessage.builder()
                    .chatId(it)
                    .text(question.toString())
                    .build()
                absSender.execute(message)
            }.onFailure { e ->
                log.warn("Could not send the message to $it", e)
            }
        }
    }
}