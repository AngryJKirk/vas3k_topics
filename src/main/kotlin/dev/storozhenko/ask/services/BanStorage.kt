package dev.storozhenko.ask.services

import com.mongodb.client.MongoClient
import org.litote.kmongo.eq
import org.litote.kmongo.findOne
import org.litote.kmongo.getCollection
import org.litote.kmongo.gt
import java.time.LocalDateTime

data class Ban(
    val userId: Long,
    val reason: String,
    val banDate: LocalDateTime
)

class BanStorage(client: MongoClient) {
    private val database = client.getDatabase("bans")
    private val banCollection = database.getCollection<Ban>()

    fun ban(userId: Long, reason: String) {
        banCollection.insertOne(Ban(userId, reason, LocalDateTime.now()))
    }

    fun isBanned(userId: Long): Ban? {
        return banCollection.findOne(Ban::userId eq userId, Ban::banDate gt LocalDateTime.now().minusDays(7))
    }
}