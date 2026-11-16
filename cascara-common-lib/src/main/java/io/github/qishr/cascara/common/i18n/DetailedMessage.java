package io.github.qishr.cascara.common.i18n;

public class DetailedMessage implements LocalizableMessage {
    private final LocalizableMessage message;
    private final Object[] details;

    public DetailedMessage(LocalizableMessage message, Object... details) {
        this.message = message;
        this.details = details;
    }

    public String name() {
        return message.name();
    }

    public String getCode() {
        return message.getCode();
    }

    public String getFormat() {
        return message.getFormat();
    }

    public Object[] getDetails() {
        return details;
    }

    public LocalizableMessage getMessage() {
        return message;
    }
}
