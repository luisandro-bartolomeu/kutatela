package ao.co.kutatelamama.controller;

import ao.co.kutatelamama.domain.entity.VaccinationRecord;
import ao.co.kutatelamama.dto.*;
import ao.co.kutatelamama.service.DashboardService;
import ao.co.kutatelamama.service.VaccinationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    private final DashboardService dashboardService;
    private final VaccinationService vaccinationService;

    public DashboardController(DashboardService dashboardService, VaccinationService vaccinationService) {
        this.dashboardService = dashboardService;
        this.vaccinationService = vaccinationService;
    }

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryDto> getSummary() {
        return ResponseEntity.ok(dashboardService.getSummaryData());
    }

    @GetMapping("/babies")
    public ResponseEntity<List<BabyDashboardDto>> getBabies(
            @RequestParam(required = false) Integer ageMonths,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(dashboardService.getBabiesForDashboard(ageMonths, status));
    }

    @PostMapping("/send-alert")
    public ResponseEntity<Map<String, Object>> sendAlert(@RequestBody SendAlertRequestDto request) {
        return ResponseEntity.ok(dashboardService.sendAlertToMother(request));
    }

    @PostMapping("/mark-vaccine-completed")
    public ResponseEntity<VaccinationRecord> markVaccineCompleted(@RequestBody MarkVaccineRequestDto request) {
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
