package ao.co.kutatelamama.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.Map;

public class WhatsAppWebhookPayloadDto {

    private String event;

    @JsonProperty("session_id")
    private String deviceId;

    private String from;
    private String sender;
    private String phone;

    private String body;
    private String message;
    private String text;

    private String id;

    private Map<String, Object> payload;

    public WhatsAppWebhookPayloadDto() {
    }

    public WhatsAppWebhookPayloadDto(String id, String from, String body, String text, String phone, String senderName) {
        this.id = id;
        this.from = from;
        this.body = body;
        this.text = text;
        this.phone = phone;
        this.sender = senderName;
    }

    // Getters and Setters
    public String getEvent() { return event; }
    public void setEvent(String event) { this.event = event; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getId() {
        if (id != null && !id.isBlank()) return id;
        if (payload != null) {
            Object idObj = payload.get("id");
            if (idObj == null) idObj = payload.get("ids");
            if (idObj != null) return idObj.toString();
        }
        return "";
    }
    public void setId(String id) { this.id = id; }

    public Map<String, Object> getPayload() { return payload; }
    public void setPayload(Map<String, Object> payload) { this.payload = payload; }

    public boolean isGroupMessage() {
        String cleanPhone = getCleanPhoneNumber();
        if (cleanPhone != null && cleanPhone.contains("@g.us")) return true;
        if (payload != null) {
            Object isGroup = payload.get("is_group");
            if (Boolean.TRUE.equals(isGroup) || "true".equalsIgnoreCase(String.valueOf(isGroup))) return true;
        }
        return false;
    }

    public String getCleanPhoneNumber() {
        if (payload != null) {
            Object fromObj = payload.get("from");
            if (fromObj != null) return fromObj.toString();

            Object senderObj = payload.get("sender");
            if (senderObj != null) return senderObj.toString();

            Object phoneObj = payload.get("phone");
            if (phoneObj != null) return phoneObj.toString();
        }

        if (from != null && !from.isBlank()) return from;
        if (sender != null && !sender.isBlank()) return sender;
        if (phone != null && !phone.isBlank()) return phone;

        return null;
    }

    public String getEffectiveText() {
        if (payload != null) {
            Object bodyObj = payload.get("body");
            if (bodyObj != null && !bodyObj.toString().isBlank()) return bodyObj.toString();

            Object msgObj = payload.get("message");
            if (msgObj != null && !msgObj.toString().isBlank()) return msgObj.toString();

            Object textObj = payload.get("text");
            if (textObj != null && !textObj.toString().isBlank()) return textObj.toString();

            Object captionObj = payload.get("caption");
            if (captionObj != null && !captionObj.toString().isBlank()) return captionObj.toString();
        }

        if (body != null && !body.isBlank()) return body;
        if (message != null && !message.isBlank()) return message;
        if (text != null && !text.isBlank()) return text;

        return null;
    }
}

