package dev.storozhenko.ask.processors

import dev.storozhenko.ask.models.Stage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.bots.AbsSender

interface StageProcessor {

    fun ownedStage(): Stage

    fun process(update: Update): (AbsSender) -> Stage

}