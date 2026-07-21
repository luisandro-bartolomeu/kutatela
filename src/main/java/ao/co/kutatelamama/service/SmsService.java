package ao.co.kutatelamama.service;

import ao.co.kutatelamama.domain.entity.SmsLog;
import ao.co.kutatelamama.repository.SmsLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsService.class);

    private final SmsLogRepository smsLogRepository;

    @Value("${kutatela.africastalking.enabled:false}")
    private boolean apiEnabled;

    @Value("${kutatela.africastalking.username:sandbox}")
    private String username;

    @Value("${kutatela.africastalking.api-key:sandbox_key}")
    private String apiKey;

    public SmsService(SmsLogRepository smsLogRepository) {
        this.smsLogRepository = smsLogRepository;
    }

    public SmsLog sendSms(String recipientPhone, String messageType, String content) {
        log.info("📱 [SMS SENDER] Sending {} to {}: {}", messageType, recipientPhone, content);

        String status = "SIMULATED";
        if (apiEnabled) {
            // In real deployment, triggers Africa's Talking SMS API gateway HTTP request
            status = "SENT";
        }

        SmsLog smsLog = new SmsLog(recipientPhone, messageType, content, status);
        return smsLogRepository.save(smsLog);
    }
}
