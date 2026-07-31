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
        try {
            initVaccines();
            initWeeklyTips();
            initSampleData();
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(DataInitializer.class)
                    .warn("[DATA_INITIALIZER] Could not pre-populate data on startup: {}", e.getMessage());
        }
    }

    private void initVaccines() {
        if (vaccineRepository.count() > 0) return;

        List<Vaccine> vaccines = List.of(
            // Ao Nascer
            new Vaccine("Pólio (Dose 0)", 0, "Poliomielite", "Dose 0 via Oral (2 gotas)."),
            new Vaccine("BCG", 0, "Tuberculose", "Dose única via Intra-Dérmica no braço esquerdo (0,05 ml)."),
            new Vaccine("Hepatite B", 0, "Hepatite B", "Dose única via Intra-Muscular na coxa esquerda (0,5 ml). Administrar até 7 dias após o nascimento."),
            // 2 Meses
            new Vaccine("Pólio (1ª dose)", 2, "Poliomielite", "1ª dose via Oral (2 gotas)."),
            new Vaccine("Rotavírus (1ª dose)", 2, "Diarreia grave por Rotavírus", "1ª dose via Oral sublingual (todo o tubo). Administrar dos 2 a <4 meses."),
            new Vaccine("Pneumococo (1ª dose)", 2, "Pneumonia, Meningite, Otite e Bronquite", "1ª dose via Intra-Muscular na coxa direita (0,5 ml)."),
            new Vaccine("Pentavalente (1ª dose)", 2, "Difteria, Tétano, Tosse Convulsa, Hib e Hepatite B", "1ª dose via Intra-Muscular na coxa esquerda (0,5 ml)."),
            // 4 Meses
            new Vaccine("Pólio (2ª dose)", 4, "Poliomielite", "2ª dose via Oral (2 gotas)."),
            new Vaccine("Rotavírus (2ª dose)", 4, "Diarreia grave por Rotavírus", "2ª dose via Oral sublingual (todo o tubo). Administrar dos 4 a <7 meses."),
            new Vaccine("Pólio Inativada (Dose Única)", 4, "Poliomielite", "Dose única via Intra-Muscular na coxa direita (0,5 ml)."),
            new Vaccine("Pneumococo (2ª dose)", 4, "Pneumonia, Meningite, Otite e Bronquite", "2ª dose via Intra-Muscular na coxa direita (0,5 ml)."),
            new Vaccine("Pentavalente (2ª dose)", 4, "Difteria, Tétano, Tosse Convulsa, Hib e Hepatite B", "2ª dose via Intra-Muscular na coxa esquerda (0,5 ml)."),
            // 6 Meses
            new Vaccine("Pólio (3ª dose)", 6, "Poliomielite", "3ª dose via Oral (2 gotas)."),
            new Vaccine("Vitamina A (1ª dose)", 6, "Suplementação de Vitamina A", "1ª dose via Oral (3 gotas)."),
            new Vaccine("Pneumococo (3ª dose)", 6, "Pneumonia, Meningite, Otite e Bronquite", "3ª dose via Intra-Muscular na coxa direita (0,5 ml)."),
            new Vaccine("Pentavalente (3ª dose)", 6, "Difteria, Tétano, Tosse Convulsa, Hib e Hepatite B", "3ª dose via Intra-Muscular na coxa esquerda (0,5 ml)."),
            // 9 Meses
            new Vaccine("Vitamina A (2ª dose)", 9, "Suplementação de Vitamina A", "2ª dose via Oral (3 gotas)."),
            new Vaccine("Sarampo/Rubéola (1ª dose)", 9, "Sarampo e Rubéola", "1ª dose via Sub-Cutânea no braço esquerdo (0,5 ml)."),
            new Vaccine("Febre Amarela", 9, "Febre Amarela", "Dose única via Sub-Cutânea no braço direito (0,5 ml)."),
            // 15 Meses
            new Vaccine("Sarampo/Rubéola (2ª dose - Reforço)", 15, "Sarampo e Rubéola", "2ª dose (reforço) via Sub-Cutânea no braço esquerdo (0,5 ml).")
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

        List<Vaccine> allVaccines = vaccineRepository.findAllByOrderByRecommendedAgeMonthsAsc();

        // Baby 1: Domingos Kiala (0 Meses) - Luanda - PENDENTE (Vacinas ao Nascer pendentes)
        Mother mother1 = new Mother("+244923111222", "Ana Kiala", "Luanda", "Cazenga", Language.PORTUGUESE);
        motherRepository.save(mother1);
        Baby baby1 = new Baby(mother1, "Domingos Kiala", "M", LocalDate.now().minusDays(10));
        babyRepository.save(baby1);
        for (Vaccine v : allVaccines) {
            LocalDate scheduledDate = baby1.getBirthDate().plusMonths(v.getRecommendedAgeMonths());
            VaccineStatus status = VaccineStatus.SCHEDULED; // Vacinas de 0M em falta
            vaccinationRecordRepository.save(new VaccinationRecord(baby1, v, scheduledDate, status, "Centro de Saúde do Cazenga"));
        }

        // Baby 2: Yara Manuel (0 Meses) - Luanda - BEM VACINADA (Vacinas ao Nascer tomadas)
        Mother mother2 = new Mother("+244925888999", "Beatriz Manuel", "Luanda", "Maianga", Language.PORTUGUESE);
        motherRepository.save(mother2);
        Baby baby2 = new Baby(mother2, "Yara Manuel", "F", LocalDate.now().minusWeeks(2));
        babyRepository.save(baby2);
        for (Vaccine v : allVaccines) {
            LocalDate scheduledDate = baby2.getBirthDate().plusMonths(v.getRecommendedAgeMonths());
            VaccineStatus status = v.getRecommendedAgeMonths() == 0 ? VaccineStatus.COMPLETED : VaccineStatus.SCHEDULED;
            vaccinationRecordRepository.save(new VaccinationRecord(baby2, v, scheduledDate, status, "Maternidade Lucrécia Paim"));
        }

        // Baby 3: Mateus Chitumba (2 Meses) - Luanda - PENDENTE
        Mother mother3 = new Mother("+244934555666", "Maria Chitumba", "Luanda", "Viana", Language.PORTUGUESE);
        motherRepository.save(mother3);
        Baby baby3 = new Baby(mother3, "Mateus Chitumba", "M", LocalDate.now().minusMonths(2).minusDays(5));
        babyRepository.save(baby3);
        for (Vaccine v : allVaccines) {
            LocalDate scheduledDate = baby3.getBirthDate().plusMonths(v.getRecommendedAgeMonths());
            VaccineStatus status = v.getRecommendedAgeMonths() == 0 ? VaccineStatus.COMPLETED : VaccineStatus.SCHEDULED;
            vaccinationRecordRepository.save(new VaccinationRecord(baby3, v, scheduledDate, status, "Hospital Materno-Infantil Azancot de Menezes"));
        }

        // Baby 4: Esperança Ngove (0 Meses) - Benguela - BEM VACINADA
        Mother mother4 = new Mother("+244941222333", "Teresa Ngove", "Benguela", "Lobito", Language.UMBUNDU);
        motherRepository.save(mother4);
        Baby baby4 = new Baby(mother4, "Esperança Ngove", "F", LocalDate.now().minusWeeks(3));
        babyRepository.save(baby4);
        for (Vaccine v : allVaccines) {
            LocalDate scheduledDate = baby4.getBirthDate().plusMonths(v.getRecommendedAgeMonths());
            VaccineStatus status = v.getRecommendedAgeMonths() == 0 ? VaccineStatus.COMPLETED : VaccineStatus.SCHEDULED;
            vaccinationRecordRepository.save(new VaccinationRecord(baby4, v, scheduledDate, status, "Hospital Geral do Lobito - Pediatria"));
        }

        // Baby 5: Ana Paula Afonso (4 Meses) - Bié - PENDENTE
        Mother mother5 = new Mother("+244912777444", "Madalena Afonso", "Bié", "Kuito", Language.PORTUGUESE);
        motherRepository.save(mother5);
        Baby baby5 = new Baby(mother5, "Ana Paula Afonso", "F", LocalDate.now().minusMonths(4).minusDays(3));
        babyRepository.save(baby5);
        for (Vaccine v : allVaccines) {
            LocalDate scheduledDate = baby5.getBirthDate().plusMonths(v.getRecommendedAgeMonths());
            VaccineStatus status = v.getRecommendedAgeMonths() <= 2 ? VaccineStatus.COMPLETED : VaccineStatus.SCHEDULED;
            vaccinationRecordRepository.save(new VaccinationRecord(baby5, v, scheduledDate, status, "Hospital Provincial do Bié"));
        }

        // Baby 6: João Baptista (6 Meses) - Uíge - BEM VACINADO
        Mother mother6 = new Mother("+244955111000", "Rosa Baptista", "Uíge", "Uíge", Language.PORTUGUESE);
        motherRepository.save(mother6);
        Baby baby6 = new Baby(mother6, "João Baptista", "M", LocalDate.now().minusMonths(6));
        babyRepository.save(baby6);
        for (Vaccine v : allVaccines) {
            LocalDate scheduledDate = baby6.getBirthDate().plusMonths(v.getRecommendedAgeMonths());
            VaccineStatus status = v.getRecommendedAgeMonths() <= 6 ? VaccineStatus.COMPLETED : VaccineStatus.SCHEDULED;
            vaccinationRecordRepository.save(new VaccinationRecord(baby6, v, scheduledDate, status, "Hospital Geral do Uíge"));
        }

        // Baby 7: Lúcio Cassoma (9 Meses) - Huíla - PENDENTE
        Mother mother7 = new Mother("+244933999111", "Helena Cassoma", "Huíla", "Lubango", Language.PORTUGUESE);
        motherRepository.save(mother7);
        Baby baby7 = new Baby(mother7, "Lúcio Cassoma", "M", LocalDate.now().minusMonths(9).minusDays(2));
        babyRepository.save(baby7);
        for (Vaccine v : allVaccines) {
            LocalDate scheduledDate = baby7.getBirthDate().plusMonths(v.getRecommendedAgeMonths());
            VaccineStatus status = v.getRecommendedAgeMonths() <= 6 ? VaccineStatus.COMPLETED : VaccineStatus.SCHEDULED;
            vaccinationRecordRepository.save(new VaccinationRecord(baby7, v, scheduledDate, status, "Centro Materno-Infantil da Humpata"));
        }
    }
}
