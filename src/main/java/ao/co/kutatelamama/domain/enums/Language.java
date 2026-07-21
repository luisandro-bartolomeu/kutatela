package ao.co.kutatelamama.domain.enums;

public enum Language {
    PORTUGUESE("Português"),
    UMBUNDU("Umbundu");

    private final String displayName;

    Language(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
