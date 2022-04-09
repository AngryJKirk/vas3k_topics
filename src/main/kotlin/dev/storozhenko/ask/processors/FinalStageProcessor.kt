package dev.storozhenko.ask.processors

import dev.storozhenko.ask.models.EditButton
import dev.storozhenko.ask.services.QuestionStorage
import dev.storozhenko.ask.services.Sender
import dev.storozhenko.ask.models.Stage
import dev.storozhenko.ask.models.Topic
import dev.storozhenko.ask.send
import dev.storozhenko.ask.toKeyboard
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard
import org.telegram.telegrambots.meta.bots.AbsSender

class FinalStageProcessor(
    private val questionStorage: QuestionStorage,
    private val sender: Sender
) : StageProcessor {
    private val startKeyboard = listOf("/start").toKeyboard()
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
                    Stage.TOPIC_EDIT
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

                    val question = questionStorage.getQuestion(update)
                    val link = sender.broadcast(update, question, it)
                    questionStorage.deleteQuestion(update)
                    it.send(
                        update,
                        "Ура, твой вопрос сейчас отправится. Чтобы отправить еще один нажми /start\n\n$link"
                    ) { replyMarkup = startKeyboard }
                    Stage.SEND
                }
            }
            EditButton.CANCEL -> {
                {
                    it.send(update, "Вопрос отменен, чтобы начать заново нажми /start") { replyMarkup = startKeyboard }
                    questionStorage.deleteQuestion(update)
                    Stage.NONE
                }
            }
        }
    }
}