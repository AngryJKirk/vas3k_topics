package dev.storozhenko.ask.services

import com.mongodb.client.MongoClient
import com.mongodb.client.model.Indexes
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

data class BoundReplies(
    val messageId: Int,
    val questionId: String,
)

class QuestionStorage(client: MongoClient) {
    private val database = client.getDatabase("questions")
    private val openQuestions = database.getCollection<QuestionWithId>("open_questions")
    private val closedQuestions = database.getCollection<QuestionWithId>("closed_questions")
    private val boundReplies =
        database.getCollection<BoundReplies>("bound_replies").apply { createIndex(Indexes.hashed("messageId")) }

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
        val question = findQuestion(update) ?: return
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

    fun findByQuestionId(questionId: String): QuestionWithId? {
        return closedQuestions.findOne(QuestionWithId::id eq questionId)
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

    fun addForwardedInfo(channelMessageId: String, forwardedMessageId: String, forwardedChatId: String) {
        closedQuestions.updateOne(
            QuestionWithId::channelMessageId eq channelMessageId,
            listOf(
                setValue(QuestionWithId::forwardedMessageId, forwardedMessageId),
                setValue(QuestionWithId::forwardedChatId, forwardedChatId)
            )
        )
    }

    fun addBoundReply(messageId: Int, questionId: String) {
        boundReplies.insertOne(BoundReplies(messageId, questionId))
    }

    fun getBoundQuestion(messageId: Int): String? {
        return boundReplies.findOne(BoundReplies::messageId eq messageId)?.questionId
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
        val foundQuestion = openQuestions.findOne { QuestionWithId::id eq key }
        val (authorId, authorName) = getAuthor(update)
        return if (foundQuestion == null) {
            val newQuestion = QuestionWithId(key, authorId = authorId.toString(), authorName = authorName)
            openQuestions.insertOne(newQuestion)
            newQuestion
        } else {
            foundQuestion
        }
    }

    private fun findQuestion(update: Update): QuestionWithId? {
        val key = getKey(update)
        return openQuestions.findOne { QuestionWithId::id eq key }
    }
}