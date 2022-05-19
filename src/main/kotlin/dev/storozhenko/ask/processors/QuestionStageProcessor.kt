package dev.storozhenko.ask.processors

import dev.storozhenko.ask.models.EditButton
import dev.storozhenko.ask.models.Stage
import dev.storozhenko.ask.send
import dev.storozhenko.ask.services.QuestionStorage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.bots.AbsSender

class QuestionStageProcessor(private val questionStorage: QuestionStorage) : StageProcessor {
    override fun ownedStage() = Stage.QUESTION

    override fun process(update: Update): (AbsSender) -> Stage {
        val message = update.message
        val text = message.text.takeIf(String::isNotBlank)
            ?: return {
                it.send(update, "Глупышка, пришли текст вопроса.")
                Stage.QUESTION
            }
        val entities = message.entities
        if (entities == null || entities.none { it.type == "hashtag" }) {
            return {
                it.send(update, "Надо добавить хотя бы один хэштег в начало или конец вопроса. Пришли текст заново.")
                Stage.QUESTION
            }
        }
        questionStorage.addQuestionText(update, text)
        return {
            it.send(update, "Теперь проверь, что все в порядке") { replyMarkup = EditButton.getKeyboard() }
            it.send(update, questionStorage.getQuestion(update).toString())
            Stage.FINAL
        }
    }
}
