package ao.co.kutatelamama.service;

import ao.co.kutatelamama.domain.entity.Baby;
import ao.co.kutatelamama.domain.entity.Mother;
import ao.co.kutatelamama.domain.entity.Vaccine;
import ao.co.kutatelamama.domain.entity.VaccinationRecord;
import ao.co.kutatelamama.domain.enums.Language;
import ao.co.kutatelamama.domain.enums.VaccineStatus;
import ao.co.kutatelamama.repository.BabyRepository;
import ao.co.kutatelamama.repository.MotherRepository;
import ao.co.kutatelamama.repository.VaccinationRecordRepository;
import ao.co.kutatelamama.repository.VaccineRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class MotherService {

    private final MotherRepository motherRepository;
    private final BabyRepository babyRepository;
    private final VaccineRepository vaccineRepository;
    private final VaccinationRecordRepository vaccinationRecordRepository;

    public MotherService(MotherRepository motherRepository,
                         BabyRepository babyRepository,
                         VaccineRepository vaccineRepository,
                         VaccinationRecordRepository vaccinationRecordRepository) {
        this.motherRepository = motherRepository;
        this.babyRepository = babyRepository;
        this.vaccineRepository = vaccineRepository;
        this.vaccinationRecordRepository = vaccinationRecordRepository;
    }

    public Optional<Mother> findByPhoneNumber(String phone) {
        return motherRepository.findByPhoneNumber(phone);
    }

    public Mother registerMotherAndBaby(String phone, String motherName, String province, String babyName, int babyAgeMonths) {
        Mother mother = motherRepository.findByPhoneNumber(phone).orElseGet(() -> {
            Mother m = new Mother(phone, motherName, province, province, Language.PORTUGUESE);
            return motherRepository.save(m);
        });

        mother.setFullName(motherName);
        mother.setProvince(province);
        motherRepository.save(mother);

        LocalDate birthDate = LocalDate.now().minusMonths(babyAgeMonths);
        Baby baby = new Baby(mother, babyName, "M", birthDate);
        Baby savedBaby = babyRepository.save(baby);

        // Schedule national vaccines
        List<Vaccine> vaccines = vaccineRepository.findAllByOrderByRecommendedAgeMonthsAsc();
        for (Vaccine v : vaccines) {
            LocalDate sched = birthDate.plusMonths(v.getRecommendedAgeMonths());
            VaccineStatus st = sched.isBefore(LocalDate.now()) ? VaccineStatus.COMPLETED : VaccineStatus.SCHEDULED;
            vaccinationRecordRepository.save(new VaccinationRecord(savedBaby, v, sched, st, "Posto de Saúde Comunitário"));
        }

        return mother;
    }

    public Baby getOrCreateDefaultBabyForMother(Mother mother) {
        return babyRepository.findFirstByMotherIdOrderByCreatedAtDesc(mother.getId())
            .orElseGet(() -> {
                Baby b = new Baby(mother, "Bebé de " + mother.getFullName(), "M", LocalDate.now().minusMonths(1));
                return babyRepository.save(b);
            });
    }
}
