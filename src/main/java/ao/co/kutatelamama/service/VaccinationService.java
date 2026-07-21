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
        sb.append("Próximas vacinas do(a) ").append(baby.getFullName()).append(":\n");

        int count = 0;
        for (VaccinationRecord rec : records) {
            if (count >= 4) break; // Limit for USSD screen size

            LocalDate today = LocalDate.now();
            long days = ChronoUnit.DAYS.between(today, rec.getScheduledDate());

            if (rec.getStatus() == VaccineStatus.COMPLETED) {
                sb.append("✅ ").append(rec.getVaccine().getName()).append(" - Realizada\n");
            } else if (days < 0) {
                sb.append("🔴 ").append(rec.getVaccine().getName()).append(" - Atrasada (").append(Math.abs(days)).append(" dias)\n");
            } else if (days == 0) {
                sb.append("⏳ ").append(rec.getVaccine().getName()).append(" - Hoje!\n");
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
               Calendário Nacional de Vacinação (PNV Angola):
               📅 Ao nascer: BCG, Polio (VIP)
               📅 2 meses: Pentavalente 1, Polio 2, Rotavírus 1
               📅 4 meses: Pentavalente 2, Polio 3, Rotavírus 2
               📅 6 meses: Pentavalente 3, Polio 4
               📅 9 meses: Febre Amarela
               📅 12 meses: Tríplice Viral (VAS)
               """;
    }

    public String getNearestHealthCenter(String province) {
        String provUpper = province != null ? province.toUpperCase() : "HUAMBO";
        if (provUpper.contains("HUAMBO")) {
            return "🏥 Unidades no Huambo:\n- Centro Materno-Infantil da Caála\n- Hospital Geral do Huambo\n- Posto de Saúde de Sanzo";
        } else if (provUpper.contains("BENGUELA")) {
            return "🏥 Unidades em Benguela:\n- Hospital Geral do Lobito\n- Centro de Saúde da Catumbela\n- Maternidade de Benguela";
        } else if (provUpper.contains("BIÉ") || provUpper.contains("BIE")) {
            return "🏥 Unidades no Bié:\n- Hospital Regional do Kuito\n- Centro de Saúde de Camacupa";
        } else if (provUpper.contains("HUÍLA") || provUpper.contains("HUILA")) {
            return "🏥 Unidades na Huíla:\n- Hospital Central do Lubango\n- Centro Materno-Infantil da Humpata";
        } else {
            return "🏥 Procure a Maternidade ou Centro de Saúde do Governo mais próximo da sua comuna/município.";
        }
    }
}
