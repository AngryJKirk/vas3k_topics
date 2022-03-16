package dev.storozhenko.ask.models

import dev.storozhenko.ask.toKeyboard
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup

enum class EditButton(val textName: String) {

    EDIT_TEXT("Изменить вопрос"),
    EDIT_TITLE("Изменить заголовок"),
    EDIT_TOPIC("Изменить тему"),
    CANCEL("Отменить вопрос"),
    DONE("Все норм, засылай");

    companion object {
        fun getByName(value: String): EditButton? {
            return values().firstOrNull { it.textName == value }
        }

        fun getKeyboard(): ReplyKeyboardMarkup {
            return values()
                .map(EditButton::textName)
                .toKeyboard(chunkSize = 1)
        }
    }
}