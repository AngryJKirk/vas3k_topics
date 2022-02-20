package dev.storozhenko.ask.processors

import dev.storozhenko.ask.EditButton
import dev.storozhenko.ask.QuestionStorage
import dev.storozhenko.ask.Stage
import dev.storozhenko.ask.StageProcessor
import dev.storozhenko.ask.send
import dev.storozhenko.ask.toKeyboard
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.bots.AbsSender

class QuestionStageProcessor(private val questionStorage: QuestionStorage) : StageProcessor {
    override fun ownedStage() = Stage.QUESTION

    override fun process(update: Update): (AbsSender) -> Stage {
        val text = update.message.text.takeIf(String::isNotBlank)
            ?: return {
                it.send(update, "Глупышка, пришли текст вопроса.")
                Stage.QUESTION
            }
        questionStorage.addQuestionText(update, text)
        return {
            it.send(update, "Теперь проверь, что все в порядке") { replyMarkup = EditButton.getKeyboard() }
            it.send(update, questionStorage.getQuestion(update).toString())
            Stage.FINAL
        }
    }
}