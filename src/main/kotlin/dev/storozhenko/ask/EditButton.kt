package dev.storozhenko.ask

enum class EditButton(val textName: String) {

    EDIT_TEXT("Изменить вопрос"),
    EDIT_TITLE("Изменить заголовок"),
    EDIT_TOPIC("Изменить тему"),
    CANCEL("Отменить вопрос"),
    DONE("Все норм, засылай");

    companion object {
        fun getByName(value: String): EditButton? {
            return EditButton.values().firstOrNull { it.textName == value }
        }
    }
}