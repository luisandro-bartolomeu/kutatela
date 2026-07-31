package ao.co.kutatelamama.service;

import ao.co.kutatelamama.domain.entity.Baby;
import ao.co.kutatelamama.domain.entity.Mother;
import ao.co.kutatelamama.domain.entity.VaccinationRecord;
import ao.co.kutatelamama.domain.entity.WeeklyTip;
import ao.co.kutatelamama.domain.enums.VaccineStatus;
import ao.co.kutatelamama.repository.BabyRepository;
import ao.co.kutatelamama.repository.MotherRepository;
import ao.co.kutatelamama.repository.VaccinationRecordRepository;
import ao.co.kutatelamama.repository.WeeklyTipRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@EnableScheduling
public class ReminderSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(ReminderSchedulerService.class);

    private final VaccinationRecordRepository vaccinationRecordRepository;
    private final WeeklyTipRepository weeklyTipRepository;
    private final MotherRepository motherRepository;
    private final BabyRepository babyRepository;
    private final SmsService smsService;
    private final WhatsAppService whatsAppService;

    public ReminderSchedulerService(VaccinationRecordRepository vaccinationRecordRepository,
                                    WeeklyTipRepository weeklyTipRepository,
                                    MotherRepository motherRepository,
                                    BabyRepository babyRepository,
                                    SmsService smsService,
                                    WhatsAppService whatsAppService) {
        this.vaccinationRecordRepository = vaccinationRecordRepository;
        this.weeklyTipRepository = weeklyTipRepository;
        this.motherRepository = motherRepository;
        this.babyRepository = babyRepository;
        this.smsService = smsService;
        this.whatsAppService = whatsAppService;
    }

    /**
     * Scheduled task running daily to send automatic alerts for all mothers with pending vaccines
     */
    @Scheduled(cron = "0 0 8 * * ?") // Every day at 8:00 AM
    public void sendAutomaticPendingVaccineAlerts() {
        log.info("[SCHEDULER] Running automatic pending vaccination alert check...");
        triggerAutomaticPendingVaccineAlerts();
    }

    /**
     * Scheduled task running daily to send SMS vaccine reminders
     */
    @Scheduled(cron = "0 0 8 * * ?") // Every day at 8:00 AM
    public void sendVaccinationReminders() {
        log.info("[SCHEDULER] Running daily vaccination reminder check...");
        triggerVaccinationReminders();
    }

    public int triggerAutomaticPendingVaccineAlerts() {
        List<Baby> babies = babyRepository.findAll();
        int alertCount = 0;
        for (Baby baby : babies) {
            List<VaccinationRecord> pending = vaccinationRecordRepository.findByBabyOrderByScheduledDateAsc(baby).stream()
                    .filter(r -> r.getVaccine().getRecommendedAgeMonths() <= baby.getAgeInMonths())
                    .filter(r -> r.getStatus() != VaccineStatus.COMPLETED)
                    .collect(java.util.stream.Collectors.toList());

            if (!pending.isEmpty() && baby.getMother() != null) {
                Mother mother = baby.getMother();
                String pendingNames = pending.stream().map(r -> r.getVaccine().getName()).collect(java.util.stream.Collectors.joining(", "));
                String message = String.format(
                    "Kutatela Mama 🌿 (Alerta Automático): Olá %s, o(a) bebé %s (%d meses) tem %d vacina(s) em falta (%s). Leve o seu bebé ao posto de saúde mais próximo!",
                    mother.getFullName(),
                    baby.getFullName(),
                    baby.getAgeInMonths(),
                    pending.size(),
                    pendingNames
                );
                whatsAppService.sendWhatsAppMessage(null, mother.getPhoneNumber(), message);
                smsService.sendSms(mother.getPhoneNumber(), "AUTO_VACCINE_ALERT", message);
                alertCount++;
            }
        }
        log.info("[SCHEDULER] Disparo automático concluído. Enviados {} alertas de vacinas pendentes via GOWA WhatsApp & SMS.", alertCount);
        return alertCount;
    }

    /**
     * Scheduled task running weekly to send maternal care tips
     */
    @Scheduled(cron = "0 0 9 * * MON") // Every Monday at 9:00 AM
    public void sendWeeklyCareTips() {
        log.info("[SCHEDULER] Running weekly maternal care tips distribution...");
        triggerWeeklyTips();
    }

    public int triggerVaccinationReminders() {
        LocalDate today = LocalDate.now();
        LocalDate targetDate = today.plusDays(5);

        List<VaccinationRecord> recordsDue = vaccinationRecordRepository.findByScheduledDateBetweenAndStatus(
                today, targetDate, VaccineStatus.SCHEDULED
        );

        int sentCount = 0;
        for (VaccinationRecord rec : recordsDue) {
            Baby baby = rec.getBaby();
            if (baby != null && baby.getMother() != null) {
                Mother mother = baby.getMother();
                String message = String.format(
                    "Kutatela Mama - Lembrete de Vacina! O(A) bebé %s tem agendada a vacina %s para o dia %s. Leve o cartão de vacinas ao centro de saúde mais próximo!",
                    baby.getFullName(),
                    rec.getVaccine().getName(),
                    rec.getScheduledDate().toString()
                );
                smsService.sendSms(mother.getPhoneNumber(), "VACCINATION_REMINDER", message);
                sentCount++;
            }
        }
        return sentCount;
    }

    public int triggerWeeklyTips() {
        List<Mother> mothers = motherRepository.findAll();
        int count = 0;

        for (Mother mother : mothers) {
            List<Baby> babies = babyRepository.findByMother(mother);
            if (!babies.isEmpty()) {
                Baby baby = babies.get(0);
                long weekAge = Math.max(1, baby.getAgeInWeeks());
                int tipIndex = (int) ((weekAge % 6) + 1);

                WeeklyTip tip = weeklyTipRepository.findByWeekNumber(tipIndex)
                        .orElseGet(() -> weeklyTipRepository.findAll().stream().findFirst().orElse(null));

                if (tip != null) {
                    String msg = String.format(
                        "Kutatela Mama: Lembrete semanal - Bebé com %d semanas! %s Cuide-se também, mãe!",
                        weekAge,
                        tip.getContentPt()
                    );
                    smsService.sendSms(mother.getPhoneNumber(), "WEEKLY_CARE_TIP", msg);
                    count++;
                }
            }
        }
        return count;
    }
}
