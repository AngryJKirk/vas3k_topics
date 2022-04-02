package dev.storozhenko.ask.models

import dev.storozhenko.ask.bold
import dev.storozhenko.ask.italic

data class Question(
    val topic: Topic,
    val title: String,
    val text: String,
    val author: Pair<Long, String>
) {

    override fun toString(): String {
        return "${topic.topicName.bold()}\n${title.bold()}\n${text.italic()}"
    }
}
