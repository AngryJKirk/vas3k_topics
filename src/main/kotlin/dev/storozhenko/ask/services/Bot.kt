package dev.storozhenko.ask.services

import dev.storozhenko.ask.chatIdString
import dev.storozhenko.ask.getLogger
import dev.storozhenko.ask.models.Stage
import dev.storozhenko.ask.name
import dev.storozhenko.ask.processors.StageProcessor
import dev.storozhenko.ask.send
import dev.storozhenko.ask.user
import org.slf4j.MDC
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatAdministrators
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberAdministrator
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberOwner

class Bot(
    private val token: String,
    private val botName: String,
    private val stageStorage: StageStorage,
    private val questionStorage: QuestionStorage,
    private val banStorage: BanStorage,
    private val helpText: String,
    stageProcessors: List<StageProcessor>
) : TelegramLongPollingBot() {
    private val log = getLogger()
    private val stageProcessorsMap = stageProcessors.associateBy(StageProcessor::ownedStage)

    override fun getBotToken() = token

    override fun getBotUsername() = botName

    override fun onUpdateReceived(update: Update) {
        val chat = update.message.chat
        if (chat.isGroupChat || chat.isChannelChat || chat.isSuperGroupChat) {
            if (update.message.hasText() && update.message.text.startsWith("/ban")) {
                processBan(update)
                return
            }
        }
        if (chat.isUserChat) {
            val userId = update.message.from.id
            MDC.putCloseable("user_id", userId.toString()).use {
                val ban = banStorage.isBanned(userId)

                if (ban != null) {
                    this.send(
                        update,
                        "\uD83D\uDEABВы забанены по причине \"${ban.reason}\", в течение недели бан спадет.\uD83D\uDEAB"
                    )
                    return
                }
                if (update.message.hasText()) {
                    if (update.message.text.startsWith("/start")) {
                        questionStorage.deleteQuestion(update)
                        stageStorage.updateStage(userId, stage = Stage.NONE)
                        this.send(update, helpText)
                    }

                    if (update.message.text.startsWith("/help")) {
                        this.send(update, helpText)
                        return
                    }
                }

                runCatching { processStages(update) }
                    .onFailure { e ->
                        log.error("Could not process message: $update", e)
                    }
            }
        }
    }

    private val adminStatuses = setOf(ChatMemberAdministrator.STATUS, ChatMemberOwner.STATUS)

    private fun processBan(update: Update) {
        val from = update.message.from
        val isFromAdmin = execute(GetChatAdministrators(update.message.chatId.toString()))
            .filter { chatMember -> chatMember.status in adminStatuses }
            .any { admin -> admin.user().id == from.id }
        if (isFromAdmin.not()) {
            this.send(update, "Только администратор может выполнить эту команду")
            return
        }
        val banMessage = update.message.text.split(" ").drop(1).joinToString(" ")
        if (banMessage.length < 10) {
            this.send(update, "В причине бана следует указать хотя бы 10 символов")
            return
        }

        if (update.message.isReply.not()) {
            this.send(update, "Необходимо ответить на сообщение бота, автора которого вы хотите забанить")
            return
        }
        if (update.message.replyToMessage.from.userName != botUsername) {
            this.send(update, "Должен быть ответ именно на сообщение бота")
            return
        }
        val replyMessage = update.message.replyToMessage
        val userToBan = replyMessage.entities.first { it.type == "text_mention" }.user
        banStorage.ban(userToBan.id, banMessage)
        val messageToDeleteUrl = replyMessage.entities.first { it.type == "text_link" }.url
        val (chatId, messageId) = messageToDeleteUrl.split("/").takeLast(2)
        runCatching { execute(DeleteMessage("-100$chatId", messageId.toInt())) }
            .onFailure(::logErrorOnDeleting)
        runCatching {
            execute(
                DeleteMessage(
                    update.chatIdString(),
                    update.message.messageId.toInt()
                )
            )
        }.onFailure {
            logErrorOnDeleting(it)
            send(update, "Не удалось удалить сообщение о бане, добавьте бота в администраторы")
        }
        send(update, "Пользователь ${userToBan.name(link = true)} забанен по причине \"$banMessage\"")
    }

    private fun processStages(update: Update) {
        val userId = update.message.from.id

        val stage = stageStorage.getCurrentStage(userId)
        log.info("Current stage is $stage")
        val processor = stageProcessorsMap[stage]
            ?: throw IllegalStateException("Must be a stage processor for all stages, stage: $stage")
        val nextStage = if (update.message.hasText()) {
            processor.process(update).invoke(this)
        } else {
            this.send(update, "Принимаем только текст, попробуй еще раз")
            processor.ownedStage()
        }
        log.info("Next stage is $nextStage")
        stageStorage.updateStage(userId, nextStage)
    }

    private fun logErrorOnDeleting(exception: Throwable) {
        log.error("Exception while deleting message: ${exception.message}", exception)
    }
}