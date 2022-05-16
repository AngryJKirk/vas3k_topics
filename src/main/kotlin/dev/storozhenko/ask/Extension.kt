package dev.storozhenko.ask

import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberAdministrator
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberBanned
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberLeft
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberMember
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberOwner
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberRestricted
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
        .apply {
            disableWebPagePreview = true
            parseMode = ParseMode.HTML
        }
    val message = messageObj.apply(customization)

    return this.execute(message)
}

fun AbsSender.send(
    chatId: String,
    text: String,
    customization: SendMessage.() -> Unit = { },
): Message {

    val messageObj = SendMessage(chatId, text)
        .apply {
            disableWebPagePreview = true
            parseMode = ParseMode.HTML
        }
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

fun String.link(link: String): String {
    return "<a href=\"$link\">$this</a>"
}

fun String.bold(): String {
    return "<b>$this</b>"
}

fun String.italic(): String {
    return "<i>${this}</i>"
}

fun ChatMember.user(): User {
    return when (status) {
        ChatMemberAdministrator.STATUS -> (this as ChatMemberAdministrator).user
        ChatMemberBanned.STATUS -> (this as ChatMemberBanned).user
        ChatMemberLeft.STATUS -> (this as ChatMemberLeft).user
        ChatMemberMember.STATUS -> (this as ChatMemberMember).user
        ChatMemberOwner.STATUS -> (this as ChatMemberOwner).user
        ChatMemberRestricted.STATUS -> (this as ChatMemberRestricted).user
        else -> throw RuntimeException("Can't find mapping for user $this")
    }
}

fun User.name(link: Boolean, nameOnly: Boolean = false): String {
    val name = if (this.userName != null && !nameOnly) {
        "@${this.userName}"
    } else if (lastName != null) {
        "${this.firstName} ${this.lastName}"
    } else {
        this.firstName
    }

    return if (link) {
        name.link("tg://user?id=${this.id}")
    } else {
        name
    }
}

fun String.cleanId(): String {
    return this.replace("-100", "")
}