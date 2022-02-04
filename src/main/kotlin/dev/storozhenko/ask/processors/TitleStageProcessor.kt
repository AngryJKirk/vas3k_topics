package dev.storozhenko.ask.processors

import dev.storozhenko.ask.QuestionStorage
import dev.storozhenko.ask.Stage
import dev.storozhenko.ask.StageProcessor
import dev.storozhenko.ask.send
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.bots.AbsSender

class TitleStageProcessor(private val questionStorage: QuestionStorage) : StageProcessor {
    override fun ownedStage() = Stage.TITLE

    override fun process(update: Update): (AbsSender) -> Stage {
        val text = update.message.text.takeIf(String::isNotBlank)
            ?: return {
                it.send(update, "Глупышка, пришли заголовок вопроса. Это должен быть текст.")
                Stage.TITLE
            }
        questionStorage.addTitle(update, text)
        return {
            it.send(update, "Теперь пришли сам текст вопроса")
            Stage.QUESTION
        }
    }
}