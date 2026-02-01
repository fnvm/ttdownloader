package org.github.fnvm.telegram;

import java.util.regex.Pattern;

import static org.github.fnvm.telegram.ActionType.*;

public class Actions {
    private static final Pattern TIKTOK_URL_PATTERN = Pattern.compile(
            "^https://(vt\\.tiktok\\.com|www\\.tiktok\\.com|vm\\.tiktok\\.com)/.*"
    );

    public static Action parse(String text) {
        if (text.startsWith("/")) {
            return parseCommand(text);
        }

        String[] parts = text.split("\\s+", 2);
        if (TIKTOK_URL_PATTERN.matcher(text).matches() && parts.length == 1) {
            return new Action(GET_SD, parts[0]);
        }
        if (parts.length == 2 && TIKTOK_URL_PATTERN.matcher(parts[0]).matches()) {
            return parseUrlWithQuality(parts[0], parts[1]);
        }

        return Action.unsupported();
    }

    private static Action parseCommand(String text) {
        String[] parts = text.split("\\s+", 2);
        String command = parts[0].toLowerCase();

        return switch (command) {
            case "/start", "/help" -> new Action(HELP, "");
            case "/setcookie" -> parseSetCookie(parts);
            case "/viewcookie", "/cookie" -> new Action(VIEW_COOKIE, "");
            case "/deletecookie", "/removecookie" -> new Action(DELETE_COOKIE, "");
            case "/get" -> parseGetCommand(parts, GET_SD);
            case "/gethd" -> parseGetCommand(parts, GET_HD);
            case "/getfull", "/getfullhd" -> parseGetCommand(parts, GET_FULLHD);
            default -> Action.unsupported();
        };
    }

    private static Action parseSetCookie(String[] parts) {
        if (parts.length < 2) {
            return new Action(SET_COOKIE, "");
        }
        return new Action(SET_COOKIE, parts[1]);
    }

    private static Action parseGetCommand(String[] parts, ActionType type) {
        if (parts.length < 2) {
            return Action.unsupported("Использование: /get <url>");
        }
        String url = parts[1].trim();
        if (!TIKTOK_URL_PATTERN.matcher(url).matches()) {
            return Action.unsupported("Некорректная ссылка");
        }
        return new Action(type, url);
    }

    private static Action parseUrlWithQuality(String url, String quality) {
        return switch (quality.toLowerCase()) {
            case "hd" -> new Action(GET_HD, url);
            case "fullhd", "full", "original" -> new Action(GET_FULLHD, url);
            default -> new Action(GET_SD, url);
        };
    }
}