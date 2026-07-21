package ao.co.kutatelamama.controller;

import ao.co.kutatelamama.domain.entity.SmsLog;
import ao.co.kutatelamama.dto.SendSmsRequestDto;
import ao.co.kutatelamama.repository.SmsLogRepository;
import ao.co.kutatelamama.service.ReminderSchedulerService;
import ao.co.kutatelamama.service.SmsService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/sms")
@CrossOrigin(origins = "*")
public class SmsController {

    private final SmsLogRepository smsLogRepository;
    private final SmsService smsService;
    private final ReminderSchedulerService reminderSchedulerService;

    public SmsController(SmsLogRepository smsLogRepository,
                         SmsService smsService,
                         ReminderSchedulerService reminderSchedulerService) {
        this.smsLogRepository = smsLogRepository;
        this.smsService = smsService;
        this.reminderSchedulerService = reminderSchedulerService;
    }

    @GetMapping("/logs")
    public ResponseEntity<List<SmsLog>> getAllSmsLogs() {
        return ResponseEntity.ok(smsLogRepository.findAllByOrderBySentAtDesc());
    }

    @GetMapping("/logs/phone/{phoneNumber}")
    public ResponseEntity<List<SmsLog>> getSmsLogsByPhone(@PathVariable String phoneNumber) {
        return ResponseEntity.ok(smsLogRepository.findByRecipientPhoneOrderBySentAtDesc(phoneNumber));
    }

    @PostMapping("/send")
    public ResponseEntity<SmsLog> sendSms(@Valid @RequestBody SendSmsRequestDto dto) {
        SmsLog log = smsService.sendSms(dto.recipientPhone(), dto.messageType(), dto.content());
        return ResponseEntity.status(HttpStatus.CREATED).body(log);
    }

    @PostMapping("/trigger-reminders")
    public ResponseEntity<Map<String, Object>> triggerReminders() {
        int vaccCount = reminderSchedulerService.triggerVaccinationReminders();
        int tipCount = reminderSchedulerService.triggerWeeklyTips();

        Map<String, Object> response = new HashMap<>();
        response.put("vaccinationRemindersSent", vaccCount);
        response.put("weeklyTipsSent", tipCount);
        response.put("status", "SUCCESS");
        return ResponseEntity.ok(response);
    }
}
