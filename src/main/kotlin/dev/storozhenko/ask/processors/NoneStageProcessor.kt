package dev.storozhenko.ask.processors

import dev.storozhenko.ask.Stage
import dev.storozhenko.ask.StageProcessor
import dev.storozhenko.ask.Topic
import dev.storozhenko.ask.send
import dev.storozhenko.ask.toKeyboard
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.bots.AbsSender

class NoneStageProcessor : StageProcessor {

    override fun ownedStage() = Stage.NONE
    override fun process(update: Update): (AbsSender) -> Stage {
        return {
            it.send(update, "Кароч вот тебе вопрос, выбери тему") {
                replyMarkup = getKeyboard()
            }

            Stage.TOPIC
        }
    }

    private fun getKeyboard(): ReplyKeyboardMarkup {
        return Topic.values()
            .map(Topic::topicName)
            .toKeyboard()
    }
}