package dev.storozhenko.ask

import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import org.telegram.telegrambots.meta.bots.AbsSender

fun AbsSender.send(
    update: Update,
    text: String,
    customization: SendMessage.() -> Unit = { },
): Message {

    val messageObj = SendMessage(update.chatIdString(), text)
    val message = messageObj.apply(customization)

    return this.execute(message)
}

fun Update.chatId(): Long {
    return when {
        hasMessage() -> message.chat.id
        hasEditedMessage() -> editedMessage.chat.id
        else -> callbackQuery.message.chat.id
    }
}

fun Update.chatIdString(): String {
    return this.chatId().toString()
}

fun Collection<String>.toKeyboard(chunkSize: Int = 2): ReplyKeyboardMarkup {
    val keyboardRows = this
        .chunked(chunkSize)
        .map { it.map(::KeyboardButton) }
        .map(::KeyboardRow)
    return ReplyKeyboardMarkup(
        keyboardRows
    ).apply {
        resizeKeyboard = true
        oneTimeKeyboard = true
    }
}
}
