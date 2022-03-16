package dev.storozhenko.ask.services

import com.mongodb.client.MongoClient
import dev.storozhenko.ask.models.Stage
import org.litote.kmongo.eq
import org.litote.kmongo.findOne
import org.litote.kmongo.getCollection
import org.litote.kmongo.insertOne
import org.litote.kmongo.setValue

data class StateWithId(val id: Long, val stage: Stage)
class StageStorage(client: MongoClient) {

    private val database = client.getDatabase("questions")
    private val col = database.getCollection<StateWithId>()

    fun getCurrentStage(userId: Long): Stage {
        val stage = col.findOne { StateWithId::id eq userId }?.stage
        return if (stage == null) {
            col.insertOne(StateWithId(userId, Stage.NONE))
            Stage.NONE
        } else {
            stage
        }
    }

    fun updateStage(userId: Long, stage: Stage) {
        col.updateOne(StateWithId::id eq userId, setValue(StateWithId::stage, stage))
    }
}