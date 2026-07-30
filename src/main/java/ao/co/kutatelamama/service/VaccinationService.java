package ao.co.kutatelamama.service;

import ao.co.kutatelamama.domain.entity.Baby;
import ao.co.kutatelamama.domain.entity.Vaccine;
import ao.co.kutatelamama.domain.entity.VaccinationRecord;
import ao.co.kutatelamama.domain.enums.VaccineStatus;
import ao.co.kutatelamama.repository.VaccinationRecordRepository;
import ao.co.kutatelamama.repository.VaccineRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class VaccinationService {

    private final VaccinationRecordRepository vaccinationRecordRepository;
    private final VaccineRepository vaccineRepository;

    public VaccinationService(VaccinationRecordRepository vaccinationRecordRepository,
                               VaccineRepository vaccineRepository) {
        this.vaccinationRecordRepository = vaccinationRecordRepository;
        this.vaccineRepository = vaccineRepository;
    }

    public String formatUpcomingVaccinesForBaby(Baby baby) {
        if (baby == null) {
            return "Nenhum bebé registado. Por favor registe o seu bebé primeiro.";
        }

        List<VaccinationRecord> records = vaccinationRecordRepository.findByBabyOrderByScheduledDateAsc(baby);

        if (records.isEmpty()) {
            return "Sem vacinas pendentes no momento.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("💉 Próximas vacinas do(a) ").append(baby.getFullName()).append(":\n");

        int count = 0;
        for (VaccinationRecord rec : records) {
            if (count >= 4) break; // Limit for USSD screen size

            LocalDate today = LocalDate.now();
            long days = ChronoUnit.DAYS.between(today, rec.getScheduledDate());

            if (rec.getStatus() == VaccineStatus.COMPLETED) {
                sb.append("✅ ").append(rec.getVaccine().getName()).append(" - Realizada\n");
            } else if (days < 0) {
                sb.append("🔴 ").append(rec.getVaccine().getName()).append(" (atraso de ").append(Math.abs(days)).append(" dias)\n");
            } else if (days == 0) {
                sb.append("⏰ ").append(rec.getVaccine().getName()).append(" - Hoje!\n");
            } else {
                sb.append("⏳ ").append(rec.getVaccine().getName()).append(" - ")
                  .append(rec.getScheduledDate().toString())
                  .append(" (em ").append(days).append(" dias)\n");
            }
            count++;
        }

        return sb.toString();
    }

    public String formatFullNationalCalendar() {
        return """
               📅 CALENDÁRIO NACIONAL DE VACINAÇÃO (MINSA 2018):
               ===========================================
               👶 AO NASCER:
               - Pólio (Dose 0): Oral (2 gotas)
               - BCG (Única): Intra-Dérmica, Braço Esquerdo (0,05 ml)
               - Hepatite B (Única): Intra-Muscular, Coxa Esquerda (0,5 ml)

               👶 2 MESES:
               - Pólio (1ª dose): Oral (2 gotas)
               - Rotavírus (1ª dose): Oral Sublingual (Todo tubo)
               - Pneumococo (1ª dose): Intra-Muscular, Coxa Direita (0,5 ml)
               - Pentavalente (1ª dose): Intra-Muscular, Coxa Esquerda (0,5 ml)

               👶 4 MESES:
               - Pólio (2ª dose): Oral (2 gotas)
               - Rotavírus (2ª dose): Oral Sublingual (Todo tubo)
               - Pólio Inativada (Única): Intra-Muscular, Coxa Direita (0,5 ml)
               - Pneumococo (2ª dose): Intra-Muscular, Coxa Direita (0,5 ml)
               - Pentavalente (2ª dose): Intra-Muscular, Coxa Esquerda (0,5 ml)

               👶 6 MESES:
               - Pólio (3ª dose): Oral (2 gotas)
               - Vitamina A (1ª dose): Oral (3 gotas)
               - Pneumococo (3ª dose): Intra-Muscular, Coxa Direita (0,5 ml)
               - Pentavalente (3ª dose): Intra-Muscular, Coxa Esquerda (0,5 ml)

               👶 9 MESES:
               - Vitamina A (2ª dose): Oral (3 gotas)
               - Sarampo/Rubéola (1ª dose): Sub-Cutânea, Braço Esquerdo (0,5 ml)
               - Febre Amarela (Única): Sub-Cutânea, Braço Direito (0,5 ml)

               👶 15 MESES:
               - Sarampo/Rubéola (2ª dose/reforço): Sub-Cutânea, Braço Esquerdo (0,5 ml)
               """;
    }

    public String formatVaccineDetailMenu() {
        return """
               📋 CONHEÇA AS VACINAS
               =====================
               1. BCG
               2. Pólio
               3. Hepatite B
               4. Pentavalente
               5. Pneumococo
               6. Rotavírus
               7. Sarampo/Rubéola
               8. Febre Amarela
               0. Voltar""";
    }

    public String formatVaccineDetail(String choice) {
        if (choice == null) return null;
        String c = choice.trim();

        switch (c) {
            case "1":
            case "BCG":
                return """
                       🔬 BCG

                       Protege contra: Tuberculose

                       Doença: A Tuberculose é uma doença causada por bactéria e muito contagiosa. Afecta principalmente os pulmões, mas pode afectar os intestinos, ossos, articulações e outros tecidos do corpo.

                       Transmissão: Pelo ar, através de tosse, espirro ou fala.

                       💉 Vacinação:
                       - Dose: Única
                       - Via: Intra-Dérmica
                       - Local: Braço Esquerdo (músculo deltoide)
                       - Quando: Ao nascer (até 1 ano)
                       - Dosagem: 0,05 ml""";

            case "2":
            case "PÓLIO":
            case "POLIO":
                return """
                       🔬 PÓLIO (POLIOMIELITE)

                       Protege contra: Pólio

                       Doença: A Pólio é causada por um vírus que afecta o sistema nervoso, deixando a pessoa aleijada e podendo levar à morte.

                       Transmissão: Contacto com fezes ou secreções respiratórias de pessoas infectadas.

                       💉 Vacinação:
                       - 4 doses: ao nascer, aos 2, 4 e 6 meses
                       - Cada dose: 2 gotas (via oral)
                       - Dose única de Pólio Inativada: aos 4 meses (via Intra-Muscular)
                       - Crianças menores de 5 anos devem apanhar a vacina contra a Pólio
                       - Durante as campanhas de vacinação, toda criança com idade menor que 5 anos, mesmo se já tenha sido vacinada, deve apanhar a vacina

                       Observação: A dose única de Pólio Inativada deve ser administrada aos 4 meses.""";

            case "3":
            case "HEPATITE B":
            case "HEPATITE_B":
                return """
                       🔬 HEPATITE B

                       Protege contra: Hepatite B

                       Doença: A Hepatite B é causada por um vírus e é muito contagiosa. É a principal causa de icterícia, doença do fígado, cirrose e cancro de fígado.

                       Transmissão: Sangue, fluidos corporais e de mãe para filho no parto.

                       💉 Vacinação:
                       - Dose: Única
                       - Via: Intra-Muscular
                       - Local: Face ânterolateral da coxa esquerda
                       - Quando: Até 7 dias após a data de nascimento
                       - Dosagem: 0,5 ml""";

            case "4":
            case "PENTAVALENTE":
                return """
                       🔬 PENTAVALENTE

                       Protege contra 5 doenças graves:

                       1️⃣ DIFTERIA:
                       Causada por toxina que atinge amígdalas e faringe. Pode obstruir a garganta e levar à morte.

                       2️⃣ TÉTANO:
                       Infecção que entra por ferimentos na pele ou pelo coto umbilical. Afecta o sistema nervoso e pode matar.

                       3️⃣ TOSSE CONVULSA:
                       Doença infecciosa que atinge o aparelho respiratório. Em crianças <6 meses, pode ser grave e fatal.

                       4️⃣ HAEMOPHILUS INFLUENZAE B:
                       Bactéria que causa meningite, sinusite e pneumonia.

                       5️⃣ HEPATITE B:
                       Vírus contagioso que causa icterícia, cirrose e cancro de fígado.

                       💉 VACINAÇÃO:
                       - 3 doses: 2, 4 e 6 meses
                       - Via: Intra-Muscular
                       - Local: Terço médio da face externa da coxa (direita/alternada)
                       - Dosagem: 0,5 ml

                       ✅ Todas as vacinas são GRATUITAS!
                       💪 Proteja o seu bebé!""";

            case "5":
            case "PNEUMOCOCUS":
            case "PNEUMOCOCO":
            case "PNEUMO":
                return """
                       🔬 PNEUMOCOCUS (PNEUMO)

                       Protege contra: Doenças causadas pela bactéria Streptococcus pneumoniae (pneumococo)

                       Doença: Causa formas graves de infecções como pneumonia, meningite, infecções do ouvido e bronquite.

                       Transmissão: Gotículas expelidas ao tossir, espirrar ou falar.

                       💉 Vacinação:
                       - 3 doses: aos 2, 4 e 6 meses
                       - Via: Intra-Muscular
                       - Local: Terço médio da face externa da coxa direita
                       - Dosagem: 0,5 ml""";

            case "6":
            case "ROTAVÍRUS":
            case "ROTAVIRUS":
                return """
                       🔬 ROTAVÍRUS

                       Protege contra: Rotavírus

                       Doença: Causada por um vírus que provoca inflamação do estômago e da parede do intestino, causando diarreia grave, acompanhada de febre e vómitos que levam rapidamente à desidratação. A diarreia por Rotavírus é muito contagiosa e atinge de forma mais grave crianças de até 2 anos de idade.

                       Transmissão: Via fecal-oral (mão-boca, água ou alimentos contaminados).

                       💉 Vacinação:
                       - 2 doses:
                         1ª dose: dos 2 meses a menores de 4 meses de idade
                         2ª dose: dos 4 meses a menores de 7 meses de idade
                       - Via: Oral
                       - Local: Sublingual
                       - Dosagem: Todo tubo

                       ⚠️ IMPORTANTE: Se uma criança estiver atrasada para vacinação, ela não pode iniciar a vacinação contra o rotavírus depois de 4 meses de idade. A segunda dose não pode ser administrada depois de 7 meses de idade.""";

            case "7":
            case "SARAMPO/RUBÉOLA":
            case "SARAMPO_RUBEOLA":
            case "SARAMPO E RUBÉOLA":
            case "SARAMPO":
                return """
                       🔬 SARAMPO E RUBÉOLA

                       Protege contra: Sarampo e Rubéola

                       Doenças:
                       - Sarampo: Doença altamente infecciosa e contagiosa. Transmitida por gotículas ao falar, tossir ou espirrar. É grave. Em grávidas, pode provocar aborto ou parto prematuro.
                       - Rubéola: Doença grave transmitida facilmente. Em mulheres grávidas, o bebé em formação pode nascer com Síndrome de Rubéola Congénita (surdez, cegueira/cataratas ou malformações do coração). Manifesta-se com febre e borbulhas na pele (cara até aos pés).

                       Prevenção: A única forma de evitar o Sarampo e a Rubéola é através da vacinação.

                       💉 Vacinação:
                       - 2 doses:
                         1ª dose: aos 9 meses
                         2ª dose (reforço): aos 15 meses
                       - Via: Sub-Cutânea
                       - Local: Região deltóide do braço esquerdo
                       - Dosagem: 0,5 ml""";

            case "8":
            case "FEBRE AMARELA":
            case "FEBRE_AMARELA":
                return """
                       🔬 FEBRE AMARELA

                       Protege contra: Febre Amarela

                       Doença: É uma febre hemorrágica causada por um vírus. Transmitida de pessoa para pessoa pela picada do mosquito Aedes aegypti infectado.

                       💉 Vacinação:
                       - Dose: Única
                       - Via: Sub-Cutânea
                       - Local: Região deltóide do braço direito
                       - Quando: Aos 9 meses
                       - Dosagem: 0,5 ml""";

            default:
                return null;
        }
    }

    public String findVaccineDescriptionByQuery(String query) {
        if (query == null || query.isBlank()) return null;
        String q = query.toLowerCase(java.util.Locale.ROOT);

        if (q.contains("bcg") || q.contains("tuberculose")) {
            return formatVaccineDetail("1");
        }
        if (q.contains("polio") || q.contains("pólio") || q.contains("poliomielite") || q.contains("paralisia")) {
            return formatVaccineDetail("2");
        }
        if (q.contains("hepatite b") || (q.contains("hepatite") && !q.contains("penta"))) {
            return formatVaccineDetail("3");
        }
        if (q.contains("penta") || q.contains("pentavalente") || q.contains("difteria") || q.contains("tétano") || q.contains("tetano") || q.contains("tosse convulsa") || q.contains("haemophilus")) {
            return formatVaccineDetail("4");
        }
        if (q.contains("pneumo") || q.contains("pneumococo") || q.contains("pneumococcus") || q.contains("streptococcus")) {
            return formatVaccineDetail("5");
        }
        if (q.contains("rotavirus") || q.contains("rotavírus") || q.contains("rota")) {
            return formatVaccineDetail("6");
        }
        if (q.contains("sarampo") || q.contains("rubeola") || q.contains("rubéola")) {
            return formatVaccineDetail("7");
        }
        if (q.contains("febre amarela") || (q.contains("amarela") && q.contains("vacina"))) {
            return formatVaccineDetail("8");
        }

        return null;
    }

    public String getNearestHealthCenter(String province) {
        String provUpper = province != null ? province.toUpperCase() : "HUAMBO";
        if (provUpper.contains("BENGO") && !provUpper.contains("BENGUELA")) {
            return "🏥 Unidades no Bengo:\n- Hospital Geral do Bengo (Caxito)\n- Centro de Saúde de Mabubas";
        } else if (provUpper.contains("BENGUELA")) {
            return "🏥 Unidades em Benguela:\n- Hospital Geral do Lobito\n- Centro de Saúde da Catumbela\n- Maternidade de Benguela";
        } else if (provUpper.contains("BIE")) {
            return "🏥 Unidades no Bié:\n- Hospital Regional do Kuito\n- Centro de Saúde de Camacupa";
        } else if (provUpper.contains("CABINDA")) {
            return "🏥 Unidades em Cabinda:\n- Hospital Geral de Cabinda\n- Centro Materno-Infantil de Cabinda";
        } else if (provUpper.contains("CUANDO")) {
            return "🏥 Unidades no Quando Cubango:\n- Hospital Geral de Menongue\n- Centro de Saúde de Calai";
        } else if (provUpper.contains("CUANZA NORTE") || provUpper.contains("KWANZA NORTE")) {
            return "🏥 Unidades no Cuanza Norte:\n- Hospital Provincial de Ndalatando\n- Centro Materno-Infantil do Cazengo";
        } else if (provUpper.contains("CUANZA SUL") || provUpper.contains("KWANZA SUL")) {
            return "🏥 Unidades no Cuanza Sul:\n- Hospital Geral do Sumbe\n- Centro de Saúde de Porto Amboim";
        } else if (provUpper.contains("CUNENE")) {
            return "🏥 Unidades no Cunene:\n- Hospital Geral de Ondjiva\n- Centro de Saúde de Namacunde";
        } else if (provUpper.contains("HUAMBO")) {
            return "🏥 Unidades no Huambo:\n- Centro Materno-Infantil da Caála\n- Hospital Geral do Huambo\n- Posto de Saúde de Sanzo";
        } else if (provUpper.contains("HUILA")) {
            return "🏥 Unidades na Huíla:\n- Hospital Central do Lubango\n- Centro Materno-Infantil da Humpata";
        } else if (provUpper.contains("LUANDA")) {
            return "🏥 Unidades em Luanda:\n- Maternidade Lucrécia Paim\n- Hospital Materno-Infantil Azancot de Menezes\n- Hospital Geral de Luanda";
        } else if (provUpper.contains("LUNDA NORTE")) {
            return "🏥 Unidades na Lunda Norte:\n- Hospital Geral de Dundo\n- Centro de Saúde de Chitato";
        } else if (provUpper.contains("LUNDA SUL")) {
            return "🏥 Unidades na Lunda Sul:\n- Hospital Geral de Saurimo\n- Centro Materno-Infantil de Saurimo";
        } else if (provUpper.contains("MALANJE")) {
            return "🏥 Unidades em Malanje:\n- Hospital Regional de Malanje\n- Centro de Saúde de Cangandala";
        } else if (provUpper.contains("MOXICO")) {
            return "🏥 Unidades no Moxico:\n- Hospital Geral de Luena\n- Centro de Saúde de Camanongue";
        } else if (provUpper.contains("NAMIBE")) {
            return "🏥 Unidades no Namibe:\n- Hospital Geral de Moçâmedes\n- Centro Materno-Infantil de Moçâmedes";
        } else if (provUpper.contains("UIGE")) {
            return "🏥 Unidades no Uíge:\n- Hospital Geral do Uíge\n- Centro Materno-Infantil do Uíge";
        } else if (provUpper.contains("ZAIRE")) {
            return "🏥 Unidades no Zaire:\n- Hospital Provincial de Mbanza Kongo\n- Centro de Saúde de Soyo";
        } else {
            return "🏥 Procure a Maternidade ou Centro de Saúde mais próximo da sua comuna ou município.";
        }
    }
}

