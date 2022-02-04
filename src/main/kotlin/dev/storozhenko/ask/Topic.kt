package dev.storozhenko.ask

enum class Topic(val topicName: String) {
    BAR("Бар"),
    TECH("Тез"),
    TRAKTOR("Трактор");

    companion object {
        fun getByName(value: String): Topic? {
            return values().firstOrNull { it.topicName == value }
        }
    }
}

