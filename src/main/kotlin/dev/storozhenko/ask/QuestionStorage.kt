package dev.storozhenko.ask

import org.telegram.telegrambots.meta.api.objects.Update

class QuestionStorage {
    private val data: MutableMap<Pair<Long, String>, MutableMap<String, String>> = mutableMapOf()

    fun addQuestionText(update: Update, text: String) {
        getMap(update)["text"] = text
    }

    fun addTopic(update: Update, topic: Topic) {
        getMap(update)["topic"] = topic.topicName
    }

    fun addTitle(update: Update, title: String) {
        getMap(update)["title"] = title
    }

    fun deleteQuestion(update: Update) {
        getMap(update).clear()
    }

    fun getQuestion(update: Update): Question {
        val map = getMap(update)
        return Question(
            Topic.getByName(map["topic"]!!)!!,
            map["title"] ?: "",
            map["text"] ?: "",
            getKey(update)
        )
    }

    private fun getMap(update: Update): MutableMap<String, String> {
        return data.computeIfAbsent(
            getKey(update)
        ) { mutableMapOf() }
    }

    private fun getKey(update: Update): Pair<Long, String> {
        val from = update.message.from
        return from.id to listOf(from.firstName, from.lastName).joinToString(separator = " ")
    }
}