package dev.storozhenko.ask.processors

import dev.storozhenko.ask.services.QuestionStorage
import dev.storozhenko.ask.models.Stage
import dev.storozhenko.ask.models.Topic
import dev.storozhenko.ask.send
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.bots.AbsSender

class TopicStageProcessor(private val questionStorage: QuestionStorage) : StageProcessor {
    override fun ownedStage() = Stage.TOPIC

    override fun process(update: Update): (AbsSender) -> Stage {
        val topic = Topic.getByName(update.message.text)
            ?: return {
                it.send(update, "Ну ты глупышка, выбери еще раз")
                Stage.TOPIC
            }
        questionStorage.addTopic(update, topic)
        return {
            it.send(update, "Теперь пришли заголовок вопроса")
            Stage.TITLE
        }
    }
}