package dev.storozhenko.ask.processors

import dev.storozhenko.ask.models.EditButton
import dev.storozhenko.ask.services.QuestionStorage
import dev.storozhenko.ask.models.Stage
import dev.storozhenko.ask.send
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.bots.AbsSender

class EditTitleStageProcessor(
    private val questionStorage: QuestionStorage
) : StageProcessor {
    override fun ownedStage() = Stage.TITLE_EDIT

    override fun process(update: Update): (AbsSender) -> Stage {
        val text = update.message.text
            ?: return {
                it.send(update, "Чтобы исправить заголовок надо ввести текст")
                Stage.TITLE_EDIT
            }
        questionStorage.addTitle(update, text)
        return {
            it.send(update, questionStorage.getQuestion(update).toString())
            it.send(update, "Заголовок исправлен, что дальше?") { replyMarkup = EditButton.getKeyboard() }
            Stage.FINAL
        }
    }
}