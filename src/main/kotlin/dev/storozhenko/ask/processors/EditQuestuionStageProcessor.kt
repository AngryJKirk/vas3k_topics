package dev.storozhenko.ask.processors

import dev.storozhenko.ask.htmlEscape
import dev.storozhenko.ask.models.EditButton
import dev.storozhenko.ask.models.Stage
import dev.storozhenko.ask.send
import dev.storozhenko.ask.services.QuestionStorage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.bots.AbsSender

class EditQuestuionStageProcessor(
    private val questionStorage: QuestionStorage
) : StageProcessor {
    override fun ownedStage() = Stage.QUESTION_EDIT

    override fun process(update: Update): (AbsSender) -> Stage {
        val message = update.message
        val text = message.text?.htmlEscape()
            ?: return {
                it.send(update, "Чтобы исправить вопрос, надо ввести текст")
                Stage.QUESTION_EDIT
            }
        val entities = message.entities
        if (entities == null || entities.none { it.type == "hashtag" }) {
            return {
                it.send(update, "Надо добавить хотя бы один хэштег в начало или конец вопроса")
                Stage.QUESTION_EDIT
            }
        }
        questionStorage.addQuestionText(update, text)
        return {
            it.send(update, questionStorage.getQuestion(update).toString())
            it.send(update, "Текст вопроса исправлен, что дальше?") { replyMarkup = EditButton.getKeyboard() }
            Stage.FINAL
        }
    }
}
