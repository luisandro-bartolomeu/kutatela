package ao.co.kutatelamama.domain.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sms_logs")
public class SmsLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String recipientPhone;

    @Column(nullable = false)
    private String messageType; // VACCINATION_REMINDER, WEEKLY_CARE_TIP, TRIAGE_SUMMARY, SYSTEM_NOTICE

    @Column(nullable = false, length = 1600)
    private String content;

    @Column(nullable = false, updatable = false)
    private LocalDateTime sentAt = LocalDateTime.now();

    @Column(nullable = false)
    private String status; // SENT, SIMULATED, FAILED

    public SmsLog() {}

    public SmsLog(String recipientPhone, String messageType, String content, String status) {
        this.recipientPhone = recipientPhone;
        this.messageType = messageType;
        this.content = content;
        this.status = status;
        this.sentAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRecipientPhone() {
        return recipientPhone;
    }

    public void setRecipientPhone(String recipientPhone) {
        this.recipientPhone = recipientPhone;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
