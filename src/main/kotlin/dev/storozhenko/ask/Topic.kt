package dev.storozhenko.ask

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import java.lang.IllegalStateException

enum class Topic(val topicName: String) {
    BAR("Бар"),
    TECH("Тех"),
    TRAKTOR("Трактор");

    companion object {
        fun getByName(value: String): Topic? {
            return values().firstOrNull { it.topicName == value }
        }

        fun getByNameNotNull(value: String): Topic {
            return values().firstOrNull { it.topicName == value }
                ?: throw IllegalStateException("Topic $value is not found")
        }

        fun getKeyboard(): ReplyKeyboardMarkup {
            return values()
                .map(Topic::topicName)
                .toKeyboard()
        }
    }
}

