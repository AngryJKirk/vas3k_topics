package dev.storozhenko.ask

import dev.storozhenko.ask.models.Topic

fun getLinkToChannel(channelId: String, channelMessageId: Int): String {
    val linkChannelId = channelId.cleanId()
    return "🔗 Ответы на вопрос в канале"
        .link("https://t.me/c/$linkChannelId/$channelMessageId?comment=1")
}

fun getLinkToChat(chatMessageId: Int, chatId: String, topic: Topic): String {
    val linkToChatId = chatId.cleanId()
    return "💬 Комментарии к вопросу в чате ${topic.topicName}"
        .link("https://t.me/c/$linkToChatId/$chatMessageId")
}