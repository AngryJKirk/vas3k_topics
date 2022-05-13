package dev.storozhenko.ask

import dev.storozhenko.ask.models.Topic
import dev.storozhenko.ask.processors.EditQuestuionStageProcessor
import dev.storozhenko.ask.processors.EditTitleStageProcessor
import dev.storozhenko.ask.processors.EditTopicStageProcessor
import dev.storozhenko.ask.processors.FinalStageProcessor
import dev.storozhenko.ask.processors.NoneStageProcessor
import dev.storozhenko.ask.processors.QuestionStageProcessor
import dev.storozhenko.ask.processors.SendStageProcessor
import dev.storozhenko.ask.processors.TitleStageProcessor
import dev.storozhenko.ask.processors.TopicStageProcessor
import dev.storozhenko.ask.services.BanStorage
import dev.storozhenko.ask.services.Bot
import dev.storozhenko.ask.services.LogStorage
import dev.storozhenko.ask.services.QuestionStorage
import dev.storozhenko.ask.services.Sender
import dev.storozhenko.ask.services.StageStorage
import org.litote.kmongo.KMongo
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

private val botToken = getEnv("TELEGRAM_API_TOKEN")
private val botUsername = getEnv("TELEGRAM_BOT_USERNAME")
private val mongoHost = getEnv("MONGO_HOST")
private val invites = getEnv("CHAT_INVITES_URL")
private val chats = getEnv("CHAT_IDS_URL")
private val channelId = getEnv("CHANNEL_ID")
private val log = LoggerFactory.getLogger("Main")

@Suppress("unused")
inline fun <reified T> T.getLogger(): Logger = LoggerFactory.getLogger(T::class.java)

fun main() {
    log.info("The application is starting")
    val mongoClient = KMongo.createClient("mongodb://$mongoHost")
    val chats = getChats()
    val banStorage = BanStorage(mongoClient)
    val telegramBotsApi = TelegramBotsApi(DefaultBotSession::class.java)
    val questionStorage = QuestionStorage(mongoClient)
    val sender = Sender(chats, channelId, questionStorage)
    val logStorage = LogStorage(mongoClient)
    val chatInvitesMap = getChatInvites()
    val processors = listOf(
        EditQuestuionStageProcessor(questionStorage),
        EditTitleStageProcessor(questionStorage),
        EditTopicStageProcessor(questionStorage),
        FinalStageProcessor(questionStorage, sender),
        NoneStageProcessor(),
        QuestionStageProcessor(questionStorage),
        TitleStageProcessor(questionStorage),
        TopicStageProcessor(questionStorage),
        SendStageProcessor()
    )

    telegramBotsApi.registerBot(
        Bot(
            botToken,
            botUsername,
            StageStorage(mongoClient),
            questionStorage,
            banStorage,
            getHelp(),
            channelId,
            chatInvitesMap,
            logStorage,
            processors
        )
    )
    log.info("The application has started")
}

private fun getEnv(envName: String): String {
    return System.getenv()[envName] ?: throw IllegalStateException("$envName does not exist")
}

private fun getHelp(): String {
    return Sender::class.java.classLoader.getResource("help.txt")?.readText()
        ?: throw IllegalStateException("Help message (help.txt) is not found in resources")
}

private fun getChatInvites(): Map<Topic, String> {
    val response = getTextByUrl(invites)
    return response.split("#")
        .map { it.split(";") }
        .associate { (topic, id) -> Topic.getByNameNotNull(topic) to id }
}

private fun getChats(): Map<Topic, List<String>> {
    val response = getTextByUrl(chats)

    return response.split("\n")
        .filter { it.isNotBlank() }
        .map { it.split(";") }
        .associate { (topic, id) -> Topic.getByNameNotNull(topic) to id.split(",") }
}

private fun getTextByUrl(url: String): String {
    val client = HttpClient.newBuilder().build()
    val request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .build()

    return client.send(request, HttpResponse.BodyHandlers.ofString()).body()
}