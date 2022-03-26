package dev.storozhenko.ask.models

import dev.storozhenko.ask.toKeyboard
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import java.lang.IllegalStateException

enum class Topic(val topicName: String) {
    TECH("🤖 Технологии"),
    FIN("💸 Финансы"),
    ABOARD("🚜 Трактор"),
    DIY("🛠 DIY"),
    CARS("🚘 Авто"),
    HEALTH("🏃‍♀️ ЗОЖ"),
    TRAVEL("⛰ Тревел"),
    COOKING("🍔 Кухня"),
    PIZDUKI("👼 Карапузы"),
    OTHER("\uD83E\uDD14 Другое");

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

