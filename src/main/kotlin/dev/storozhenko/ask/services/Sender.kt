package dev.storozhenko.ask.services

import dev.storozhenko.ask.getLinkToChannel
import dev.storozhenko.ask.getLinkToChat
import dev.storozhenko.ask.getLogger
import dev.storozhenko.ask.models.Question
import dev.storozhenko.ask.models.Topic
import dev.storozhenko.ask.send
import org.telegram.telegrambots.meta.api.methods.ParseMode
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
        val execute = absSender.send(channelId, question.toString())
        val channelMessageId = execute.messageId
        val linkToChannel = getLinkToChannel(channelId, channelMessageId)
        questionStorage.addChannelMessageId(update, channelMessageId.toString())
        val chatId = chats[question.topic]?.first() ?: return linkToChannel
        runCatching {
            val message = absSender.send(chatId, question.toStringWithoutTopic() + "\n\n" + linkToChannel + "\n\n ↩️ Вы можете ответить реплаем на это сообщение. Ваш ответ перешлется в канал.")
            val chatMessageId = message.messageId
            val linkToChat = getLinkToChat(chatMessageId, chatId, question.topic)
            questionStorage.addChatMessageId(update, chatMessageId.toString(), chatId)
            absSender.execute(
                EditMessageText.builder()
                    .chatId(channelId)
                    .messageId(channelMessageId)
                    .parseMode(ParseMode.HTML)
                    .text(question.toString() + "\n\n" + linkToChat)
                    .build()
            )
        }.onFailure { e ->
            log.warn("Could not send the message to $chatId", e)
        }
        return linkToChannel
    }
}
