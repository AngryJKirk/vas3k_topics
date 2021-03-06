package dev.storozhenko.ask.models

import dev.storozhenko.ask.bold
import dev.storozhenko.ask.italic
import dev.storozhenko.ask.link

data class Question(
    val topic: Topic,
    val title: String,
    val text: String,
    val author: Pair<Long, String>,
    val chatMessageId: String? = null,
    val channelMessageId: String? = null
) {

    override fun toString(): String {
        return listOf(
            topic.topicName.bold(),
            title.bold() + " от ${author.second.link("tg://user?id=${author.first}")}",
            text.italic()
        ).joinToString("\n\n")
    }

    fun toStringWithoutTopic(): String {
        return listOf(
            title.bold() + " от ${author.second.link("tg://user?id=${author.first}")}",
            text.italic()
        ).joinToString("\n\n")
    }
}
