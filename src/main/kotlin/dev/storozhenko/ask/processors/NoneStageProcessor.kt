package dev.storozhenko.ask.processors

import dev.storozhenko.ask.models.Stage
import dev.storozhenko.ask.models.Topic
import dev.storozhenko.ask.send
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.bots.AbsSender

class NoneStageProcessor : StageProcessor {

    override fun ownedStage() = Stage.NONE
    override fun process(update: Update): (AbsSender) -> Stage {
        return {
            it.send(update, "Выбери тему вопроса") {
                replyMarkup = Topic.getKeyboard()
            }
            Stage.TOPIC
        }
    }

}