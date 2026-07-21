package ao.co.kutatelamama.domain.enums;

public enum AlarmLevel {
    NORMAL("Normal", "💚"),
    WARNING("Atenção", "⚠️"),
    URGENT("Urgente", "🚨");

    private final String title;
    private final String emoji;

    AlarmLevel(String title, String emoji) {
        this.title = title;
        this.emoji = emoji;
    }

    public String getTitle() {
        return title;
    }

    public String getEmoji() {
        return emoji;
    }
}
