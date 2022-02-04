package dev.storozhenko.ask

import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.bots.AbsSender

interface StageProcessor {

    fun ownedStage(): Stage

    fun process(update: Update): (AbsSender) -> Stage

}