package dev.storozhenko.ask.services

import com.mongodb.client.MongoClient
import dev.storozhenko.ask.models.Question
import dev.storozhenko.ask.models.Topic
import org.litote.kmongo.and
import org.litote.kmongo.eq
import org.litote.kmongo.findOne
import org.litote.kmongo.getCollection
import org.litote.kmongo.setValue
import org.telegram.telegrambots.meta.api.objects.Update

data class QuestionWithId(
    val id: String,
    val authorId: String,
    val authorName: String,
    val question: String? = null,
    val topic: String? = null,
    val title: String? = null,
    val channelMessageId: String? = null,
    val chatMessageId: String? = null,
    val forwardedMessageId: String? = null,
    val forwardedChatId: String? = null,
    val chatId: String? = null,
)

class QuestionStorage(client: MongoClient) {
    private val database = client.getDatabase("questions")
    private val openQuestions = database.getCollection<QuestionWithId>("open_questions")
    private val closedQuestions = database.getCollection<QuestionWithId>("closed_questions")

    fun addQuestionText(update: Update, text: String) {
        val question = findOrCreate(update)
        openQuestions.updateOne(QuestionWithId::id eq question.id, setValue(QuestionWithId::question, text))
    }

    fun addTopic(update: Update, topic: Topic) {
        val question = findOrCreate(update)
        openQuestions.updateOne(QuestionWithId::id eq question.id, setValue(QuestionWithId::topic, topic.topicName))
    }

    fun addTitle(update: Update, title: String) {
        val question = findOrCreate(update)
        openQuestions.updateOne(QuestionWithId::id eq question.id, setValue(QuestionWithId::title, title))
    }

    fun deleteQuestion(update: Update) {
        val question = findOrCreate(update)
        closedQuestions.insertOne(question)
        openQuestions.deleteOne(QuestionWithId::id eq getKey(update))
    }

    fun addChatMessageId(update: Update, chatMessageId: String, chatId: String) {
        val question = findOrCreate(update)
        openQuestions.updateOne(
            QuestionWithId::id eq question.id,
            listOf(
                setValue(QuestionWithId::chatMessageId, chatMessageId),
                setValue(QuestionWithId::chatId, chatId)
            )
        )
    }

    fun addChannelMessageId(update: Update, channelMessageId: String) {
        val question = findOrCreate(update)
        openQuestions.updateOne(
            QuestionWithId::id eq question.id,
            setValue(QuestionWithId::channelMessageId, channelMessageId)
        )
    }

    fun findByChatMessageId(chatId: String, chatMessageId: String): QuestionWithId? {
        return closedQuestions.findOne(
            and(
                QuestionWithId::chatMessageId eq chatMessageId,
                QuestionWithId::chatId eq chatId
            )
        )
    }

    fun findByChannelMessageId(channelMessageId: String): QuestionWithId? {
        return closedQuestions.findOne(QuestionWithId::channelMessageId eq channelMessageId)
    }

    fun getQuestion(update: Update): Question {
        val questionWithId = findOrCreate(update)
        return Question(
            Topic.getByName(questionWithId.topic!!)!!,
            questionWithId.title ?: "",
            questionWithId.question ?: "",
            getAuthor(update),
            questionWithId.chatMessageId,
            questionWithId.channelMessageId
        )
    }

    private fun getKey(update: Update): String {
        val (id, name) = getAuthor(update)
        return "$id $name"
    }

    private fun getAuthor(update: Update): Pair<Long, String> {
        val from = update.message.from
        val name = listOfNotNull(from.firstName, from.lastName).joinToString(separator = " ")
        return from.id to name
    }

    private fun findOrCreate(update: Update): QuestionWithId {
        val key = getKey(update)
        val (authorId, authorName) = getAuthor(update)
        val foundQuestion = openQuestions.findOne { QuestionWithId::id eq key }
        return if (foundQuestion == null) {
            val newQuestion = QuestionWithId(key, authorId = authorId.toString(), authorName = authorName)
            openQuestions.insertOne(newQuestion)
            newQuestion
        } else {
            foundQuestion
        }
    }

    fun addForwardedInfo(channelMessageId: String, forwardedMessageId: String, forwardedChatId: String) {
        closedQuestions.updateOne(
            QuestionWithId::channelMessageId eq channelMessageId,
            listOf(
                setValue(QuestionWithId::forwardedMessageId, forwardedMessageId),
                setValue(QuestionWithId::forwardedChatId, forwardedChatId)
            )
        )
    }
}