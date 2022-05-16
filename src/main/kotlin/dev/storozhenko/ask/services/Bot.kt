package dev.storozhenko.ask.services

import dev.storozhenko.ask.bold
import dev.storozhenko.ask.chatIdString
import dev.storozhenko.ask.cleanId
import dev.storozhenko.ask.getLogger
import dev.storozhenko.ask.italic
import dev.storozhenko.ask.link
import dev.storozhenko.ask.models.Stage
import dev.storozhenko.ask.models.Topic
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
    private val channelId: String,
    private val chatInvites: Map<Topic, String>,
    private val logStorage: LogStorage,
    stageProcessors: List<StageProcessor>
) : TelegramLongPollingBot() {
    private val telegramInternalUserId = 777000L
    private val log = getLogger()
    private val stageProcessorsMap = stageProcessors.associateBy(StageProcessor::ownedStage)
    override fun getBotToken() = token

    override fun getBotUsername() = botName

    override fun onUpdateReceived(update: Update) {
        log.info("Received update: $update")
        logStorage.save(update)
        MDC.put("chatId", update.message?.chatId?.toString())
        MDC.put("userId", update.message?.from?.id?.toString())
        runCatching { processUpdate(update) }.onFailure {
            log.error("Error processing update: $update", it)
        }
        MDC.clear()
    }

    private fun processUpdate(update: Update) {
        val chat = update.message?.chat
        if (chat == null) {
            log.info("Received update without chat")
            return
        }
        if (update.message.from.id == telegramInternalUserId) {
            processForwardUpdate(update)
            return
        }
        if (chat.isGroupChat || chat.isChannelChat || chat.isSuperGroupChat) {
            processGroupChat(update)
        }
        if (chat.isUserChat) {
            processUserChat(update)
        }
    }

    private fun processForwardUpdate(update: Update) {
        val forwardFromMessageId = update.message.forwardFromMessageId?.toString()
        if (forwardFromMessageId == null) {
            log.info("Received update without forwardFromMessageId")
            return
        }
        questionStorage.addForwardedInfo(
            forwardFromMessageId,
            update.message.messageId.toString(),
            update.message.chatId.toString()
        )
    }

    private fun processGroupChat(update: Update) {
        val message = update.message
        if (message.hasText() && message.text.startsWith("/ban")) {
            processBan(update)
            log.info("Banned user ${message.from.id}")
        }
        if (message.hasText() && message.isReply) {
            val replyToMessage = message.replyToMessage
            if (replyToMessage.from.userName == botUsername) {
                processChatReply(update)
                log.info("Processed chat reply")
            }
            if (replyToMessage.isAutomaticForward == true) {
                processChannelReply(update)
                log.info("Processed channel reply")
            }
            val possibleQuestionId = questionStorage.getBoundQuestion(replyToMessage.messageId)
            if (possibleQuestionId != null) {
                processChannelReply(update, possibleQuestionId)
            }
        }
    }

    private fun processUserChat(update: Update) {
        val message = update.message
        val userId = message.from.id
        MDC.putCloseable("user_id", userId.toString()).use {
            val ban = banStorage.isBanned(userId)

            if (ban != null) {
                this.send(
                    update,
                    "\uD83D\uDEABВы забанены по причине \"${ban.reason}\", в течение недели бан спадет.\uD83D\uDEAB"
                )
                log.info("User $userId is banned, shall not pass")
                return
            }
            if (message.hasText()) {
                if (message.text.startsWith("/start")) {
                    log.info("User (re)started the bot")
                    questionStorage.deleteQuestion(update)
                    stageStorage.updateStage(userId, stage = Stage.NONE)
                    this.send(update, helpText)
                }

                if (message.text.startsWith("/help")) {
                    log.info("User requested help")
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

    private fun processChannelReply(update: Update, questionId: String? = null) {
        val messageId = update.message.replyToMessage.forwardFromMessageId.toString()
        val question = if (questionId == null) {
            questionStorage.findByChannelMessageId(messageId)
        } else {
            questionStorage.findByQuestionId(questionId)
        }

        if (question == null) {
            log.info("Could not find question for channel reply, messageId: $messageId")
            return
        }
        questionStorage.addBoundReply(update.message.messageId, question.id)
        processReply(question, update, inviteLink = false)
    }

    private fun processChatReply(update: Update) {
        val message = update.message
        val channelMessageId = message.replyToMessage.messageId.toString()
        val question = questionStorage.findByChatMessageId(
            message.chat.id.toString(),
            channelMessageId
        )
        if (question == null) {
            log.info("Could not find question for chat reply, chatId: ${message.chat.id}, channelMessageId: $channelMessageId")
            return
        }

        processReply(question, update, inviteLink = true)
        forwardToChannelChat(question, update)
    }

    private fun processReply(question: QuestionWithId, update: Update, inviteLink: Boolean) {
        val message = update.message
        val responseChatId = message.chat.id.toString().cleanId()
        val responseMessageId = message.messageId.toString()
        val responseLink = "\uD83D\uDCACответ".link("https://t.me/c/$responseChatId/$responseMessageId")
        val questionLink =
            "❓ Ссылка на ваш вопрос".link("https://t.me/c/${channelId.cleanId()}/${question.channelMessageId}?comment=1")
        val responseAuthor = message.from.name(link = true, nameOnly = true)
        val textToSend =
            if (inviteLink) {
                val chatInviteLink = getChatInviteLink(question)
                "Вам $responseLink на вопрос \"${question.title?.bold()}\" от $responseAuthor из чата $chatInviteLink:\n\n ${message.text.italic()}" +
                    "\n\n" + questionLink
            } else {
                "Вам $responseLink на вопрос \"${question.title?.bold()}\" от $responseAuthor из чата канала:\n\n ${message.text.italic()}" +
                    "\n\n" + questionLink
            }
        send(update, textToSend)
    }

    private fun forwardToChannelChat(question: QuestionWithId, update: Update) {
        val forwardedChatId = question.forwardedChatId
        if (forwardedChatId == null) {
            log.info("Could not find forwarded chat id for question: $question")
            return
        }
        val responseChatId = update.message.chat.id.toString().cleanId()
        val responseMessageId = update.message.messageId.toString()
        val responseLink = "\uD83D\uDCACОтвет".link("https://t.me/c/$responseChatId/$responseMessageId")
        val responseAuthor = update.message.from.name(link = true, nameOnly = true)
        val chatLink = getChatInviteLink(question)
        val message =
            "$responseLink от $responseAuthor из чата $chatLink:\n\n${update.message.text.italic()}"
        send(update, message) {
            replyToMessageId = question.forwardedMessageId?.toInt()
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

    private fun getChatInviteLink(question: QuestionWithId): String {
        if (question.topic == null) {
            throw IllegalStateException("Question must have topic")
        }
        val topic = Topic.getByNameNotNull(question.topic)
        val inviteLink = chatInvites[topic] ?: throw IllegalStateException("No chat invite for topic $topic")
        return topic.topicName.link(inviteLink)
    }
}