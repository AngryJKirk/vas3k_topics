import dev.storozhenko.ask.Bot
import dev.storozhenko.ask.StageStorage
import dev.storozhenko.ask.processors.CancelStageProcessor
import dev.storozhenko.ask.processors.EditQuestuionStageProcessor
import dev.storozhenko.ask.processors.EditTitleStageProcessor
import dev.storozhenko.ask.processors.EditTopicStageProcessor
import dev.storozhenko.ask.processors.FinalStageProcessor
import dev.storozhenko.ask.processors.NoneStageProcessor
import dev.storozhenko.ask.processors.QuestionStageProcessor
import dev.storozhenko.ask.processors.SendStageProcessor
import dev.storozhenko.ask.processors.TitleStageProcessor
import dev.storozhenko.ask.processors.TopicStageProcessor
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession

private val botToken = getEnv("TELEGRAM_API_TOKEN")
private val botUsername = getEnv("TELEGRAM_BOT_USERNAME")

fun main() {
    val telegramBotsApi = TelegramBotsApi(DefaultBotSession::class.java)
    val processors = listOf(
        CancelStageProcessor(),
        EditQuestuionStageProcessor(),
        EditTitleStageProcessor(),
        EditTopicStageProcessor(),
        FinalStageProcessor(),
        NoneStageProcessor(),
        QuestionStageProcessor(),
        SendStageProcessor(),
        TitleStageProcessor(),
        TopicStageProcessor()
    )
    telegramBotsApi.registerBot(Bot(botToken, botUsername, StageStorage(), processors))
}

private fun getEnv(envName: String): String {
    return System.getenv()[envName] ?: throw IllegalStateException("$envName does not exist")
}