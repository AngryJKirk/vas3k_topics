package dev.storozhenko.ask

class StageStorage {

    private val storage: MutableMap<Long, Stage> = mutableMapOf()

    fun getCurrentStage(userId: Long): Stage {
        return storage[userId] ?: Stage.NONE
    }

    fun updateStage(userId: Long, stage: Stage) {
        storage[userId] = stage
    }
}