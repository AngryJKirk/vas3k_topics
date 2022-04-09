package dev.storozhenko.ask.services

import com.mongodb.client.MongoClient
import org.litote.kmongo.getCollection
import org.telegram.telegrambots.meta.api.objects.Update

class LogStorage(mongoClient: MongoClient) {

    private val collection = mongoClient.getDatabase("log").getCollection<Update>("logs")

    fun save(update: Update) {
        collection.insertOne(update)
    }
}