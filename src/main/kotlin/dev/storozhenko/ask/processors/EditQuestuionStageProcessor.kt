package dev.storozhenko.ask.processors

import dev.storozhenko.ask.Stage
import dev.storozhenko.ask.StageProcessor
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.bots.AbsSender

class EditQuestuionStageProcessor : StageProcessor {
    override fun ownedStage(): Stage {
        TODO("Not yet implemented")
    }

    override fun process(update: Update): (AbsSender) -> Stage {
        TODO("Not yet implemented")
    }
}