package org.github.fnvm.telegram.action;

public enum ActionType {
    SET_COOKIE("Установка cookies"),
    VIEW_COOKIE("Просмотр cookies"),
    DELETE_COOKIE("Удаление cookies"),
    GET_SD("Скачать SD"),
    GET_HD("Скачать HD"),
    GET_FULLHD("Скачать FullHD"),
    HELP("Помощь"),
    UNSUPPORTED("Неподдерживаемая команда");

    private final String description;

    ActionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}