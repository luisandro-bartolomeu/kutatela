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
    private final VaccinationService vaccinationService;

    public MotherService(MotherRepository motherRepository,
                         BabyRepository babyRepository,
                         VaccineRepository vaccineRepository,
                         VaccinationRecordRepository vaccinationRecordRepository,
                         VaccinationService vaccinationService) {
        this.motherRepository = motherRepository;
        this.babyRepository = babyRepository;
        this.vaccineRepository = vaccineRepository;
        this.vaccinationRecordRepository = vaccinationRecordRepository;
        this.vaccinationService = vaccinationService;
    }

    public String normalizePhoneNumber(String phone) {
        if (phone == null || phone.isBlank()) return "+244923000000";
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.startsWith("00244")) {
            digits = digits.substring(2);
        }
        if (digits.length() == 9) {
            return "+244" + digits;
        } else if (digits.length() == 12 && digits.startsWith("244")) {
            return "+" + digits;
        } else if (!digits.isEmpty()) {
            return "+" + digits;
        }
        return phone.trim();
    }

    public Optional<Mother> findByPhoneNumber(String rawPhone) {
        if (rawPhone == null || rawPhone.isBlank()) return Optional.empty();
        String normalized = normalizePhoneNumber(rawPhone);
        Optional<Mother> found = motherRepository.findByPhoneNumber(normalized);
        if (found.isPresent()) return found;

        // Fallback: busca pelos últimos 9 dígitos para vincular registros inseridos por USSD/WhatsApp com formatações diferentes
        String digitsOnly = rawPhone.replaceAll("[^0-9]", "");
        if (digitsOnly.length() >= 9) {
            String last9 = digitsOnly.substring(digitsOnly.length() - 9);
            Optional<Mother> matched = motherRepository.findAll().stream()
                .filter(m -> m.getPhoneNumber() != null && m.getPhoneNumber().replaceAll("[^0-9]", "").endsWith(last9))
                .findFirst();

            if (matched.isPresent()) {
                Mother m = matched.get();
                m.setPhoneNumber(normalized);
                motherRepository.save(m);
                return Optional.of(m);
            }
        }
        return Optional.empty();
    }

    public Mother registerMotherAndBaby(String phone, String motherName, String province, String babyName, int babyAgeMonths) {
        String normalized = normalizePhoneNumber(phone);
        String targetProvince = (province != null && !province.isBlank()) ? province : "Luanda";
        String targetMotherName = (motherName != null && !motherName.isBlank()) ? motherName : ("Mãe " + normalized.substring(Math.max(0, normalized.length() - 4)));
        String targetBabyName = (babyName != null && !babyName.isBlank()) ? babyName : ("Bebé de " + targetMotherName);
        int validAge = Math.max(0, babyAgeMonths);

        Mother mother = findByPhoneNumber(normalized).orElseGet(() -> {
            Mother m = new Mother(normalized, targetMotherName, targetProvince, targetProvince, Language.PORTUGUESE);
            return motherRepository.save(m);
        });

        mother.setPhoneNumber(normalized);
        if (motherName != null && !motherName.isBlank()) {
            mother.setFullName(targetMotherName);
        }
        if (province != null && !province.isBlank()) {
            mother.setProvince(targetProvince);
            mother.setMunicipality(targetProvince);
        }
        motherRepository.save(mother);

        Baby baby = getOrCreateDefaultBabyForMother(mother);
        baby.setFullName(targetBabyName);
        baby.setBirthDate(LocalDate.now().minusMonths(validAge));
        baby = babyRepository.save(baby);
        vaccinationService.ensureVaccinationRecordsExist(baby);

        return mother;
    }

    public Mother updateMotherName(Mother mother, String motherName) {
        mother.setFullName(motherName);
        motherRepository.save(mother);

        Baby baby = getOrCreateDefaultBabyForMother(mother);
        if (baby.getFullName() == null || baby.getFullName().startsWith("Bebé de ")) {
            baby.setFullName("Bebé de " + motherName);
            babyRepository.save(baby);
        }
        return mother;
    }

    public Mother updateProvince(Mother mother, String province) {
        String targetProvince = (province != null && !province.isBlank()) ? province : "Luanda";
        mother.setProvince(targetProvince);
        mother.setMunicipality(targetProvince);
        motherRepository.save(mother);
        return mother;
    }

    public Mother updateBabyAge(Mother mother, int babyAgeMonths) {
        Baby baby = getOrCreateDefaultBabyForMother(mother);
        baby.setBirthDate(LocalDate.now().minusMonths(Math.max(0, babyAgeMonths)));
        baby = babyRepository.save(baby);
        vaccinationService.ensureVaccinationRecordsExist(baby);
        return mother;
    }

    public Baby getOrCreateDefaultBabyForMother(Mother mother) {
        Baby baby = babyRepository.findFirstByMotherIdOrderByCreatedAtDesc(mother.getId())
            .orElseGet(() -> {
                Baby b = new Baby(mother, "Bebé de " + mother.getFullName(), "M", LocalDate.now());
                return babyRepository.save(b);
            });
        vaccinationService.ensureVaccinationRecordsExist(baby);
        return baby;
    }
}
