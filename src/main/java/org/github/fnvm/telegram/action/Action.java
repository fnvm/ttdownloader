package org.github.fnvm.telegram.action;

public record Action(ActionType type, String payload, String originalMessage) {

    public Action(ActionType type, String payload) {
        this(type, payload, "");
    }

    public boolean hasPayload() {
        return payload != null && !payload.isBlank();
    }

    public static Action unsupported() {
        return new Action(ActionType.UNSUPPORTED, "", "");
    }

    public static Action unsupported(String message) {
        return new Action(ActionType.UNSUPPORTED, "", message);
    }
}