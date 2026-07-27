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
               📅 Calendário Nacional de Vacinação (PNV Angola):
               - Ao nascer: BCG, Pólio (VIP)
               - 2 meses: Pentavalente 1, Pólio 2, Rotavírus 1
               - 4 meses: Pentavalente 2, Pólio 3, Rotavírus 2
               - 6 meses: Pentavalente 3, Pólio 4
               - 9 meses: Febre Amarela
               - 12 meses: Tríplice Viral (VAS)
               """;
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
