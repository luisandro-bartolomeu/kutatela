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
        Mother mother = findByPhoneNumber(normalized).orElseGet(() -> {
            Mother m = new Mother(normalized, motherName, province, province, Language.PORTUGUESE);
            return motherRepository.save(m);
        });

        mother.setPhoneNumber(normalized);
        mother.setFullName(motherName);
        mother.setProvince(province);
        motherRepository.save(mother);

        Baby baby = getOrCreateDefaultBabyForMother(mother);
        baby.setFullName(babyName);
        baby.setBirthDate(LocalDate.now().minusMonths(babyAgeMonths));
        babyRepository.save(baby);

        return mother;
    }

    public Mother updateMotherName(Mother mother, String motherName) {
        mother.setFullName(motherName);
        motherRepository.save(mother);

        Baby baby = getOrCreateDefaultBabyForMother(mother);
        if (baby.getFullName() == null || baby.getFullName().startsWith("Bebe de ")) {
            baby.setFullName("Bebe de " + motherName);
            babyRepository.save(baby);
        }
        return mother;
    }

    public Mother updateProvince(Mother mother, String province) {
        mother.setProvince(province);
        mother.setMunicipality(province);
        motherRepository.save(mother);
        return mother;
    }

    public Mother updateBabyAge(Mother mother, int babyAgeMonths) {
        Baby baby = getOrCreateDefaultBabyForMother(mother);
        baby.setBirthDate(LocalDate.now().minusMonths(babyAgeMonths));
        babyRepository.save(baby);
        return mother;
    }

    public Baby getOrCreateDefaultBabyForMother(Mother mother) {
        return babyRepository.findFirstByMotherIdOrderByCreatedAtDesc(mother.getId())
            .orElseGet(() -> {
                Baby b = new Baby(mother, "Bebe de " + mother.getFullName(), "M", LocalDate.now().minusMonths(1));
                return babyRepository.save(b);
            });
    }
}
