package ao.co.kutatelamama.controller;

import ao.co.kutatelamama.domain.entity.VaccinationRecord;
import ao.co.kutatelamama.dto.*;
import ao.co.kutatelamama.service.DashboardService;
import ao.co.kutatelamama.service.ReminderSchedulerService;
import ao.co.kutatelamama.service.VaccinationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    private final DashboardService dashboardService;
    private final VaccinationService vaccinationService;
    private final ReminderSchedulerService reminderSchedulerService;

    public DashboardController(DashboardService dashboardService,
                               VaccinationService vaccinationService,
                               ReminderSchedulerService reminderSchedulerService) {
        this.dashboardService = dashboardService;
        this.vaccinationService = vaccinationService;
        this.reminderSchedulerService = reminderSchedulerService;
    }

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryDto> getSummary() {
        return ResponseEntity.ok(dashboardService.getSummaryData());
    }

    @GetMapping("/babies")
    public ResponseEntity<List<BabyDashboardDto>> getBabies(
            @RequestParam(required = false) Integer ageMonths,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String province) {
        return ResponseEntity.ok(dashboardService.getBabiesForDashboard(ageMonths, status, province));
    }

    @PostMapping("/send-alert")
    public ResponseEntity<Map<String, Object>> sendAlert(@RequestBody SendAlertRequestDto request) {
        return ResponseEntity.ok(dashboardService.sendAlertToMother(request));
    }

    @PostMapping("/send-global-alert")
    public ResponseEntity<Map<String, Object>> sendGlobalAlert(@RequestBody(required = false) Map<String, String> request) {
        String channel = request != null ? request.get("channel") : "WHATSAPP";
        String message = request != null ? request.get("message") : null;
        return ResponseEntity.ok(dashboardService.sendGlobalAlertToOverdueMothers(channel, message));
    }

    @PostMapping("/trigger-auto-alerts")
    public ResponseEntity<Map<String, Object>> triggerAutoAlerts() {
        int count = reminderSchedulerService.triggerAutomaticPendingVaccineAlerts();
        Map<String, Object> res = new HashMap<>();
        res.put("status", "SUCCESS");
        res.put("message", "Disparo automático de alertas concluído com sucesso.");
        res.put("alertsSent", count);
        return ResponseEntity.ok(res);
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
