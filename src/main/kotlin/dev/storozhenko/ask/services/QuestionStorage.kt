package dev.storozhenko.ask.services

import com.mongodb.client.MongoClient
import dev.storozhenko.ask.models.Question
import dev.storozhenko.ask.models.Topic
import org.litote.kmongo.KMongo
import org.litote.kmongo.eq
import org.litote.kmongo.findOne
import org.litote.kmongo.getCollection
import org.litote.kmongo.insertOne
import org.litote.kmongo.setValue
import org.litote.kmongo.updateOne
import org.telegram.telegrambots.meta.api.objects.Update

data class QuestionWithId(
    val id: String,
    val question: String? = null,
    val topic: String? = null,
    val title: String? = null
)

class QuestionStorage(client: MongoClient) {
    private val database = client.getDatabase("questions")
    private val col = database.getCollection<QuestionWithId>()

    fun addQuestionText(update: Update, text: String) {
        val question = findOrCreate(update)
        col.updateOne(QuestionWithId::id eq question.id, setValue(QuestionWithId::question, text))
    }

    fun addTopic(update: Update, topic: Topic) {
        val question = findOrCreate(update)
        col.updateOne(QuestionWithId::id eq question.id, setValue(QuestionWithId::topic, topic.topicName))
    }

    fun addTitle(update: Update, title: String) {
        val question = findOrCreate(update)
        col.updateOne(QuestionWithId::id eq question.id, setValue(QuestionWithId::title, title))
    }

    fun deleteQuestion(update: Update) {
        col.deleteOne(QuestionWithId::id eq getKey(update))
    }

    fun getQuestion(update: Update): Question {
        val questionWithId = findOrCreate(update)
        return Question(
            Topic.getByName(questionWithId.topic!!)!!,
            questionWithId.title ?: "",
            questionWithId.question ?: "",
            getAuthor(update)
        )
    }

    private fun getKey(update: Update): String {
        val (id, name) = getAuthor(update)
        return "$id $name"
    }

    private fun getAuthor(update: Update): Pair<Long, String> {
        val from = update.message.from
        return from.id to listOf(from.firstName, from.lastName).joinToString(separator = " ")
    }

    private fun findOrCreate(update: Update): QuestionWithId {
        val key = getKey(update)
        val foundQuestion = col.findOne { QuestionWithId::id eq key }
        return if (foundQuestion == null) {
            val newQuestion = QuestionWithId(key)
            col.insertOne(newQuestion)
            newQuestion
        } else {
            foundQuestion
        }
    }
}