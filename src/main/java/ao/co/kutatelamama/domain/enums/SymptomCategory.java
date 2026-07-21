package ao.co.kutatelamama.domain.enums;

public enum SymptomCategory {
    CHORO_PERSISTENTE("Choro persistente"),
    BORBULHAS_ERUPCOES("Borbulhas e erupções cutâneas"),
    FEBRE("Febre"),
    DIARREIA_VOMITOS("Diarreia e vómitos"),
    DIFICULDADE_MAMAR("Dificuldade para mamar"),
    OUTRO("Outros sintomas");

    private final String description;

    SymptomCategory(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
