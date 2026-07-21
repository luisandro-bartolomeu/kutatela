package ao.co.kutatelamama.domain.enums;

public enum VaccineStatus {
    SCHEDULED("Agendada", "⏳"),
    COMPLETED("Realizada", "✅"),
    OVERDUE("Atrasada", "🔴");

    private final String label;
    private final String icon;

    VaccineStatus(String label, String icon) {
        this.label = label;
        this.icon = icon;
    }

    public String getLabel() {
        return label;
    }

    public String getIcon() {
        return icon;
    }
}
