package dev.storozhenko.ask.services

import dev.storozhenko.ask.getLogger
import dev.storozhenko.ask.models.Stage
import dev.storozhenko.ask.processors.StageProcessor
import dev.storozhenko.ask.send
import org.slf4j.MDC
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.objects.Update

class Bot(
    private val token: String,
    private val botName: String,
    private val stageStorage: StageStorage,
    private val questionStorage: QuestionStorage,
    private val banList: Set<Long>,
    stageProcessors: List<StageProcessor>
) : TelegramLongPollingBot() {
    private val log = getLogger()
    private val stageProcessorsMap = stageProcessors.associateBy(StageProcessor::ownedStage)

    override fun getBotToken() = token

    override fun getBotUsername() = botName

    override fun onUpdateReceived(update: Update) {
        if (update.message.chat.isUserChat) {
            val userId = update.message.from.id
            MDC.putCloseable("user_id", userId.toString()).use {
                if (userId in banList) {
                    this.send(update, "\uD83D\uDEABВы в бане\uD83D\uDEAB")
                }
                if (update.message.hasText() && update.message.text.startsWith("/start")) {
                    questionStorage.deleteQuestion(update)
                    stageStorage.updateStage(userId, stage = Stage.NONE)
                }
                runCatching { processStages(update) }
                    .onFailure { e ->
                        log.error("Could not process message: $update", e)
                    }
            }
        }
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
}