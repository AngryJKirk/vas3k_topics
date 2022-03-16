package dev.storozhenko.ask.processors

import dev.storozhenko.ask.models.Stage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.bots.AbsSender

class SendStageProcessor : StageProcessor {
    private val noneStageProcessor = NoneStageProcessor()
    override fun ownedStage() = Stage.SEND

    override fun process(update: Update): (AbsSender) -> Stage {
        return noneStageProcessor.process(update)
    }
}