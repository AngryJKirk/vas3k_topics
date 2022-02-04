package dev.storozhenko.ask

import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.objects.Update

class Bot(
    private val token: String,
    private val botName: String,
    private val stageStorage: StageStorage,
    stageProcessors: List<StageProcessor>
) : TelegramLongPollingBot() {

    private val stageProcessorsMap = stageProcessors.associateBy(StageProcessor::ownedStage)

    override fun getBotToken() = token

    override fun getBotUsername() = botName

    override fun onUpdateReceived(update: Update) {
        if (update.message.chat.isUserChat) {
            processStages(update)
        }
    }

    private fun processStages(update: Update) {
        val userId = update.message.from.id
        val stage = stageStorage.getCurrentStage(userId)
        val processor = stageProcessorsMap[stage]
            ?: throw IllegalStateException("Must be a stage processor for all stages, stage: $stage")
        val nextStage = processor.process(update).invoke(this)
        stageStorage.updateStage(userId, nextStage)
    }
}