package ao.co.kutatelamama.config;

import ao.co.kutatelamama.domain.entity.*;
import ao.co.kutatelamama.domain.enums.Language;
import ao.co.kutatelamama.domain.enums.VaccineStatus;
import ao.co.kutatelamama.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final VaccineRepository vaccineRepository;
    private final WeeklyTipRepository weeklyTipRepository;
    private final MotherRepository motherRepository;
    private final BabyRepository babyRepository;
    private final VaccinationRecordRepository vaccinationRecordRepository;

    public DataInitializer(VaccineRepository vaccineRepository,
                           WeeklyTipRepository weeklyTipRepository,
                           MotherRepository motherRepository,
                           BabyRepository babyRepository,
                           VaccinationRecordRepository vaccinationRecordRepository) {
        this.vaccineRepository = vaccineRepository;
        this.weeklyTipRepository = weeklyTipRepository;
        this.motherRepository = motherRepository;
        this.babyRepository = babyRepository;
        this.vaccinationRecordRepository = vaccinationRecordRepository;
    }

    @Override
    public void run(String... args) {
        initVaccines();
        initWeeklyTips();
        initSampleData();
    }

    private void initVaccines() {
        if (vaccineRepository.count() > 0) return;

        List<Vaccine> vaccines = List.of(
            new Vaccine("BCG", 0, "Tuberculose", "Dose única ao nascer. Protege contra formas graves de tuberculose (meningite tuberculosa)."),
            new Vaccine("Polio (VIP)", 0, "Poliomielite (Paralisia Infantil)", "Dose de nascimento via oral ou injetável."),
            new Vaccine("Pentavalente 1ª Dose", 2, "Difteria, Tétano, Coqueluche, Hepatite B, Hib", "Protege contra 5 doenças graves. Aplicar aos 2 meses."),
            new Vaccine("Polio 2ª Dose", 2, "Poliomielite", "Segunda dose da vacina contra paralisia infantil."),
            new Vaccine("Rotavírus 1ª Dose", 2, "Diarreia grave por Rotavírus", "Previne diarreias e desidratação grave em recém-nascidos."),
            new Vaccine("Pentavalente 2ª Dose", 4, "Difteria, Tétano, Coqueluche, Hepatite B, Hib", "Segunda dose de reforço."),
            new Vaccine("Polio 3ª Dose", 4, "Poliomielite", "Terceira dose da vacina contra paralisia infantil."),
            new Vaccine("Rotavírus 2ª Dose", 4, "Diarreia grave por Rotavírus", "Segunda dose oral."),
            new Vaccine("Pentavalente 3ª Dose", 6, "Difteria, Tétano, Coqueluche, Hepatite B, Hib", "Terceira dose de imunização completa."),
            new Vaccine("Polio 4ª Dose", 6, "Poliomielite", "Quarta dose."),
            new Vaccine("Febre Amarela", 9, "Febre Amarela", "Dose essencial para zonas tropicais como Angola."),
            new Vaccine("Tríplice Viral (VAS)", 12, "Sarampo, Rubéola, Caxumba", "Dose de 1 ano de vida para proteção contra surtos.")
        );

        vaccineRepository.saveAll(vaccines);
    }

    private void initWeeklyTips() {
        if (weeklyTipRepository.count() > 0) return;

        List<WeeklyTip> tips = List.of(
            new WeeklyTip(1, "AMAMENTACAO", "Amamentacao Exclusiva",
                "O colostro (primeiro leite) e a primeira vacina do bebe! De apenas leite materno, sem agua ou cha. Amamente sempre que o bebe pedir.",
                "Ovele yososo yondamba ya moso yimue yetu! Echa owele wo ina likamwe, ka ku ka kuate ovava ale sha chae."),
            new WeeklyTip(2, "HIGIENE", "Cuidados com o Coto Umbilical",
                "Mantenha o umbigo do bebe limpo e seco. Lave as maos antes de tocar. Nao coloque moedas, faixas ou substancias caseiras.",
                "Sukula o-umbigo yo m̃õmbe i kuate eke limue. Ka ku ka kuate olombongo ale ovima vikuavo."),
            new WeeklyTip(4, "SONO_SEGURO", "Sono Seguro do Recem-Nascido",
                "Deite o bebe sempre de barriga para cima numa superficie firme. Mantenha o espaco sem almofadas soltas ou mantas pesadas.",
                "Pekisa o-m̃õmbe vo menda yene okuti imba eye peke. Ka ku ka kuata oyombondo via pama."),
            new WeeklyTip(8, "ESTIMULACAO", "Estimulacao e Desenvolvimento",
                "O seu bebe ja comeca a sorrir e a seguir rostos com os olhos! Converse com ele suavemente e cante cancoes de embalar.",
                "O-m̃õmbe yove ya tunda oku yolula! Vansulula laye komuenyo umue uwa laye."),
            new WeeklyTip(12, "VACINACAO", "Lembrete de Vacinacao",
                "Mantenha o cartao de vacinas em dia! Procure o Posto de Saude mais proximo para verificar as vacinas.",
                "Enda k'osipitali oco o-m̃õmbe yove i kuate o-vacina yaye yo 4 ovosãi!"),
            new WeeklyTip(24, "NUTRICAO", "Nutricao e Introducao Alimentar",
                "Aos 6 meses pode iniciar papinhas de pirao fino, legumes amassados e frutas locais, continuando a amamentar.",
                "Cali eye otembo yo ku echa yulia yakua funa p'osipitali, momo echa ko leka okulia wo pirão fine.")
        );

        weeklyTipRepository.saveAll(tips);
    }

    private void initSampleData() {
        if (motherRepository.count() > 0) return;

        // Sample Mother 1: Maria Chitumba from Huambo (Caála)
        Mother mother1 = new Mother("+244923111222", "Maria Chitumba", "Huambo", "Caála", Language.PORTUGUESE);
        motherRepository.save(mother1);

        Baby baby1 = new Baby(mother1, "Mateus Chitumba", "M", LocalDate.now().minusMonths(2).minusDays(5));
        babyRepository.save(baby1);

        // Add vaccination records for Baby 1
        List<Vaccine> allVaccines = vaccineRepository.findAllByOrderByRecommendedAgeMonthsAsc();
        for (Vaccine v : allVaccines) {
            LocalDate scheduledDate = baby1.getBirthDate().plusMonths(v.getRecommendedAgeMonths());
            VaccineStatus status = VaccineStatus.SCHEDULED;
            LocalDate administered = null;

            if (v.getRecommendedAgeMonths() == 0) {
                status = VaccineStatus.COMPLETED;
                administered = baby1.getBirthDate();
            } else if (v.getRecommendedAgeMonths() <= 2 && scheduledDate.isBefore(LocalDate.now())) {
                status = VaccineStatus.SCHEDULED; // Due soon
            }

            vaccinationRecordRepository.save(new VaccinationRecord(
                baby1, v, scheduledDate, status, "Centro de Saúde Materno-Infantil da Caála"
            ));
        }

        // Sample Mother 2: Teresa Ngove from Benguela (Lobito)
        Mother mother2 = new Mother("+244934555666", "Teresa Ngove", "Benguela", "Lobito", Language.UMBUNDU);
        motherRepository.save(mother2);

        Baby baby2 = new Baby(mother2, "Esperança Ngove", "F", LocalDate.now().minusWeeks(3));
        babyRepository.save(baby2);

        for (Vaccine v : allVaccines) {
            LocalDate scheduledDate = baby2.getBirthDate().plusMonths(v.getRecommendedAgeMonths());
            VaccineStatus status = VaccineStatus.SCHEDULED;
            LocalDate administered = null;

            if (v.getRecommendedAgeMonths() == 0) {
                status = VaccineStatus.COMPLETED;
                administered = baby2.getBirthDate();
            }

            vaccinationRecordRepository.save(new VaccinationRecord(
                baby2, v, scheduledDate, status, "Hospital Geral do Lobito - Pediatria"
            ));
        }
    }
}
