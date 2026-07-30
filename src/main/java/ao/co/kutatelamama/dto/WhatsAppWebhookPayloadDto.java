package ao.co.kutatelamama.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.Map;

public class WhatsAppWebhookPayloadDto {

    private String event;
    private String type;

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

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

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

    /**
     * Verifica se a mensagem e do tipo localizacao ("location").
     */
    public boolean isLocationMessage() {
        if ("location".equalsIgnoreCase(this.type)) {
            return true;
        }
        if (payload != null) {
            Object typeObj = payload.get("type");
            if (typeObj != null && "location".equalsIgnoreCase(typeObj.toString().trim())) {
                return true;
            }
            Object msgTypeObj = payload.get("message_type");
            if (msgTypeObj != null && "location".equalsIgnoreCase(msgTypeObj.toString().trim())) {
                return true;
            }
        }
        return getLatitude() != null && getLongitude() != null;
    }

    /**
     * Extrai a latitude do objeto da mensagem do GoWA com seguranca.
     */
    public Double getLatitude() {
        return extractCoordinate("latitude", "degreesLatitude", "lat");
    }

    /**
     * Extrai a longitude do objeto da mensagem do GoWA com seguranca.
     */
    public Double getLongitude() {
        return extractCoordinate("longitude", "degreesLongitude", "lng", "lon", "long");
    }

    private Double extractCoordinate(String... targetKeys) {
        if (payload == null) return null;

        // 1. Procura diretamente no payload
        for (String key : targetKeys) {
            if (payload.containsKey(key)) {
                Double val = parseCoordinate(payload.get(key));
                if (val != null) return val;
            }
        }

        // 2. Procura em sub-objetos comuns do GoWA (location, locationMessage, message)
        String[] subMapKeys = {"location", "locationMessage", "message"};
        for (String subKey : subMapKeys) {
            Object sub = payload.get(subKey);
            if (sub instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) sub;
                for (String key : targetKeys) {
                    if (map.containsKey(key)) {
                        Double val = parseCoordinate(map.get(key));
                        if (val != null) return val;
                    }
                }
                // Procura um nivel mais fundo se 'message' contiver 'locationMessage' ou 'location'
                if ("message".equals(subKey)) {
                    Object nestedLoc = map.get("locationMessage");
                    if (nestedLoc == null) nestedLoc = map.get("location");
                    if (nestedLoc instanceof Map) {
                        Map<?, ?> nestedMap = (Map<?, ?>) nestedLoc;
                        for (String key : targetKeys) {
                            if (nestedMap.containsKey(key)) {
                                Double val = parseCoordinate(nestedMap.get(key));
                                if (val != null) return val;
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    private Double parseCoordinate(Object val) {
        if (val == null) return null;
        if (val instanceof Number) {
            return ((Number) val).doubleValue();
        }
        try {
            return Double.parseDouble(val.toString().trim());
        } catch (Exception e) {
            return null;
        }
    }
}
