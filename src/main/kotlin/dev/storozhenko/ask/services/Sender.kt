package dev.storozhenko.ask.services

import dev.storozhenko.ask.getLogger
import dev.storozhenko.ask.link
import dev.storozhenko.ask.models.Question
import dev.storozhenko.ask.models.Topic
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageMedia
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
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
            .parseMode("Markdown")
            .build()
        val channelMessageId = absSender.execute(channelMessage).messageId
        if (question.topic == Topic.OTHER) {
            return
        }
        // ебаная хуйня так как ссылка только на последнее сообщение,
        // все работает покуда для каждого топика только один чат
        val chatIds = chats[question.topic] ?: return
        chatIds.forEach {
            runCatching {
                val message = SendMessage.builder()
                    .chatId(it)
                    .parseMode("Markdown")
                    .text(question.toString() + "\n\n" + getLinkToChannel(channelMessageId))
                    .build()
                val chatMessageId = absSender.execute(message).messageId
                val linkToChat = getLinkToChat(chatMessageId, it, question.topic)

                absSender.execute(
                    EditMessageText.builder()
                        .chatId(channelId)
                        .messageId(channelMessageId)
                        .parseMode("Markdown")
                        .text(question.toString() + "\n\n" + linkToChat)
                        .build()
                )
            }.onFailure { e ->
                log.warn("Could not send the message to $it", e)
            }
        }
    }

    private fun getLinkToChannel(channelMessageId: Int): String {
        val linkChannelId = channelId.replace("-100", "")
        return "🔗 Ответы на вопрос в канале"
            .link("https://t.me/c/$linkChannelId/$channelMessageId")
    }

    private fun getLinkToChat(chatMessageId: Int, chatId: String, topic: Topic): String {
        val linkToChatId = chatId.replace("-100", "")
        return "💬 Комментарии к вопросу в чате ${topic.topicName}"
            .link("https://t.me/c/$linkToChatId/$chatMessageId")
    }
}
