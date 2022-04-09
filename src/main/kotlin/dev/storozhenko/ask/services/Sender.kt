package dev.storozhenko.ask.services

import dev.storozhenko.ask.getLinkToChannel
import dev.storozhenko.ask.getLinkToChat
import dev.storozhenko.ask.getLogger
import dev.storozhenko.ask.models.Question
import dev.storozhenko.ask.models.Topic
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.bots.AbsSender

class Sender(
    private val chats: Map<Topic, List<String>>,
    private val channelId: String,
    private val questionStorage: QuestionStorage
) {
    private val log = getLogger()

    fun broadcast(update: Update, question: Question, absSender: AbsSender): String {
        val channelMessage = SendMessage.builder()
            .chatId(channelId)
            .text(question.toString())
            .parseMode("Markdown")
            .build()
        val channelMessageId = absSender.execute(channelMessage).messageId
        val linkToChannel = getLinkToChannel(channelId, channelMessageId)
        questionStorage.addChannelMessageId(update, channelMessageId.toString())
        val chatId = chats[question.topic]?.first() ?: return linkToChannel
        runCatching {
            val message = SendMessage.builder()
                .chatId(chatId)
                .parseMode("Markdown")
                .text(question.toString() + "\n\n" + linkToChannel)
                .build()
            val chatMessageId = absSender.execute(message).messageId
            val linkToChat = getLinkToChat(chatMessageId, chatId, question.topic)
            questionStorage.addChatMessageId(update, chatMessageId.toString(), chatId)
            absSender.execute(
                EditMessageText.builder()
                    .chatId(channelId)
                    .messageId(channelMessageId)
                    .parseMode("Markdown")
                    .text(question.toString() + "\n\n" + linkToChat)
                    .build()
            )
        }.onFailure { e ->
            log.warn("Could not send the message to $chatId", e)
        }
        return linkToChannel
    }
}
