package dev.storozhenko.ask

import dev.storozhenko.ask.processors.EditQuestuionStageProcessor
import dev.storozhenko.ask.processors.EditTitleStageProcessor
import dev.storozhenko.ask.processors.EditTopicStageProcessor
import dev.storozhenko.ask.processors.FinalStageProcessor
import dev.storozhenko.ask.processors.NoneStageProcessor
import dev.storozhenko.ask.processors.QuestionStageProcessor
import dev.storozhenko.ask.processors.TitleStageProcessor
import dev.storozhenko.ask.processors.TopicStageProcessor
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession

private val botToken = getEnv("TELEGRAM_API_TOKEN")
private val botUsername = getEnv("TELEGRAM_BOT_USERNAME")

fun main() {
    val chats = getChats()
    val channelId = getResource("channels.csv")
    val sender = Sender(chats, channelId)
    val telegramBotsApi = TelegramBotsApi(DefaultBotSession::class.java)
    val questionStorage = QuestionStorage()
    val processors = listOf(
        EditQuestuionStageProcessor(questionStorage),
        EditTitleStageProcessor(questionStorage),
        EditTopicStageProcessor(questionStorage),
        FinalStageProcessor(questionStorage, sender),
        NoneStageProcessor(),
        QuestionStageProcessor(questionStorage),
        TitleStageProcessor(questionStorage),
        TopicStageProcessor(questionStorage)
    )
    telegramBotsApi.registerBot(Bot(botToken, botUsername, StageStorage(), processors))
}

private fun getChats() = getResource("chats.csv").lines()
    .map { it.split(";") }
    .associate { (topic, id) -> Topic.getByNameNotNull(topic) to id.split(",") }

private fun getEnv(envName: String): String {
    return System.getenv()[envName] ?: throw IllegalStateException("$envName does not exist")
}

private fun getResource(name: String): String {
    return Sender::class.java.classLoader.getResource(name)?.readText()
        ?: throw IllegalStateException("Resource $name is not found")
}