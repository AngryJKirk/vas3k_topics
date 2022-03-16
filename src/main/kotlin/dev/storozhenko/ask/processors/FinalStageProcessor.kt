package dev.storozhenko.ask.processors

import dev.storozhenko.ask.models.EditButton
import dev.storozhenko.ask.services.QuestionStorage
import dev.storozhenko.ask.services.Sender
import dev.storozhenko.ask.models.Stage
import dev.storozhenko.ask.models.Topic
import dev.storozhenko.ask.send
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.bots.AbsSender

class FinalStageProcessor(
    private val questionStorage: QuestionStorage,
    private val sender: Sender
) : StageProcessor {
    override fun ownedStage() = Stage.FINAL

    override fun process(update: Update): (AbsSender) -> Stage {
        val editButton = EditButton.getByName(update.message.text) ?: return {
            it.send(update, "Глупышка, выбери одно из действий")
            ownedStage()
        }
        return when (editButton) {
            EditButton.EDIT_TEXT -> {
                {
                    it.send(update, "Давай поправим текст")
                    Stage.QUESTION_EDIT
                }
            }
            EditButton.EDIT_TOPIC -> {
                {
                    it.send(update, "Давай поправим тему вопроса") { replyMarkup = Topic.getKeyboard() }
                    Stage.TITLE_EDIT
                }
            }
            EditButton.EDIT_TITLE -> {
                {
                    it.send(update, "Давай поправим заголовок")
                    Stage.TITLE_EDIT
                }
            }
            EditButton.DONE -> {
                {
                    it.send(update, "Ура, твой вопрос сейчас отправится. Чтобы отправить еще один нажми /start")
                    val question = questionStorage.getQuestion(update)
                    sender.broadcast(question, it)
                    questionStorage.deleteQuestion(update)
                    Stage.SEND
                }
            }
            EditButton.CANCEL -> {
                {
                    it.send(update, "Вопрос отменен, чтобы начать заново нажми /start")
                    questionStorage.deleteQuestion(update)
                    Stage.NONE
                }
            }
        }
    }
}