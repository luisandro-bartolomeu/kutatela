package ao.co.kutatelamama.controller;

import ao.co.kutatelamama.domain.entity.Vaccine;
import ao.co.kutatelamama.domain.entity.VaccinationRecord;
import ao.co.kutatelamama.repository.VaccineRepository;
import ao.co.kutatelamama.repository.VaccinationRecordRepository;
import ao.co.kutatelamama.service.VaccinationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/vaccinations")
@CrossOrigin(origins = "*")
public class VaccinationController {

    private final VaccineRepository vaccineRepository;
    private final VaccinationRecordRepository vaccinationRecordRepository;
    private final VaccinationService vaccinationService;

    public VaccinationController(VaccineRepository vaccineRepository,
                                 VaccinationRecordRepository vaccinationRecordRepository,
                                 VaccinationService vaccinationService) {
        this.vaccineRepository = vaccineRepository;
        this.vaccinationRecordRepository = vaccinationRecordRepository;
        this.vaccinationService = vaccinationService;
    }

    @GetMapping("/vaccines")
    public ResponseEntity<List<Vaccine>> getAllVaccines() {
        return ResponseEntity.ok(vaccineRepository.findAllByOrderByRecommendedAgeMonthsAsc());
    }

    @GetMapping("/national-calendar")
    public ResponseEntity<Map<String, Object>> getNationalCalendar() {
        Map<String, Object> res = new HashMap<>();
        res.put("calendarText", vaccinationService.formatFullNationalCalendar());
        res.put("vaccines", vaccineRepository.findAllByOrderByRecommendedAgeMonthsAsc());
        return ResponseEntity.ok(res);
    }

    @GetMapping("/baby/{babyId}")
    public ResponseEntity<List<VaccinationRecord>> getBabyVaccinations(@PathVariable Long babyId) {
        return ResponseEntity.ok(vaccinationRecordRepository.findByBabyIdOrderByScheduledDateAsc(babyId));
    }

    @PostMapping("/mark-completed")
    public ResponseEntity<VaccinationRecord> markVaccineCompleted(@RequestBody ao.co.kutatelamama.dto.MarkVaccineRequestDto request) {
        if (request.getRecordId() != null) {
            VaccinationRecord updated = vaccinationService.markVaccineCompleted(
                request.getRecordId(), request.getAdministeredDate(), request.getHealthCenterName()
            );
            return ResponseEntity.ok(updated);
        } else if (request.getBabyId() != null && request.getVaccineId() != null) {
            VaccinationRecord updated = vaccinationService.markVaccineCompletedByBabyAndVaccine(
                request.getBabyId(), request.getVaccineId(), request.getAdministeredDate(), request.getHealthCenterName()
            );
            return ResponseEntity.ok(updated);
        } else {
            throw new IllegalArgumentException("Deve fornecer recordId ou (babyId e vaccineId)");
        }
    }
}
