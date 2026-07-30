package ao.co.kutatelamama.controller;

import ao.co.kutatelamama.dto.WhatsAppWebhookPayloadDto;
import ao.co.kutatelamama.service.LocationService;
import ao.co.kutatelamama.service.WhatsAppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/whatsapp")
@Tag(name = "WhatsApp Webhook", description = "Endpoints de integração com Gowa / Whatsmeow para WhatsApp")
public class WhatsAppController {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppController.class);

    private final WhatsAppService whatsAppService;
    private final LocationService locationService;

    public WhatsAppController(WhatsAppService whatsAppService, LocationService locationService) {
        this.whatsAppService = whatsAppService;
        this.locationService = locationService;
    }

    @GetMapping("/webhook")
    @Operation(summary = "Endpoint de verificação de saúde/webhook do WhatsApp")
    public ResponseEntity<Map<String, Object>> verifyWhatsAppWebhook(
            @RequestParam(value = "hub.challenge", required = false) String challenge) {
        if (challenge != null && !challenge.isBlank()) {
            return ResponseEntity.ok(Map.of("hub.challenge", challenge, "status", "ONLINE"));
        }
        return ResponseEntity.ok(Map.of("status", "ONLINE", "service", "Kutatela Mama WhatsApp API Gateway"));
    }

    @PostMapping("/webhook")
    @Operation(summary = "Webhook do Gowa para receção de mensagens do WhatsApp")
    public ResponseEntity<Map<String, Object>> handleWhatsAppWebhook(
            @RequestBody WhatsAppWebhookPayloadDto payload,
            @RequestHeader(value = "X-Device-Id", required = false) String headerDeviceId,
            @RequestParam(value = "device_id", required = false) String queryDeviceId) {

        // Se for confirmação de leitura ou entrega, descarta imediatamente sem processar
        String event = payload.getEvent();
        if (event != null && (event.startsWith("message.ack") || event.equalsIgnoreCase("read") || event.equalsIgnoreCase("receipt") || event.equalsIgnoreCase("ack"))) {
            return ResponseEntity.ok(Map.of("status", "IGNORED", "message", "Evento de ack/leitura ignorado"));
        }

        // Descarta mensagens de grupo
        if (payload.isGroupMessage()) {
            return ResponseEntity.ok(Map.of("status", "IGNORED", "message", "Mensagem de grupo ignorada"));
        }

        // Resolve qual DeviceID usar (Prioridade: JSON session_id -> Header -> Query)
        String deviceId = payload.getDeviceId();
        if (deviceId == null || deviceId.isBlank()) {
            if (headerDeviceId != null && !headerDeviceId.isBlank()) {
                deviceId = headerDeviceId;
            } else if (queryDeviceId != null && !queryDeviceId.isBlank()) {
                deviceId = queryDeviceId;
            }
        }
        payload.setDeviceId(deviceId);

        // Interceção para mensagens de localização nativa do WhatsApp
        if (payload.isLocationMessage()) {
            Double lat = payload.getLatitude();
            Double lon = payload.getLongitude();
            String phone = payload.getCleanPhoneNumber();

            log.info("📍 [WHATSAPP LOCATION] From: '{}', Lat: {}, Lon: {}, DeviceId: '{}'", phone, lat, lon, deviceId);

            if (phone != null && lat != null && lon != null) {
                String locationResponseMessage = locationService.findNearestHealthCentersMessage(lat, lon);
                whatsAppService.sendWhatsAppMessage(deviceId, phone, locationResponseMessage);
                return ResponseEntity.ok(Map.of(
                        "status", "SUCCESS",
                        "message", "Localização processada e resposta de postos de saúde enviada",
                        "messageId", payload.getId()
                ));
            }
        }

        log.info("📩 [WHATSAPP WEBHOOK] Message ID: '{}', From: '{}', Text: '{}', DeviceId: '{}'",
                payload.getId(), payload.getCleanPhoneNumber(), payload.getEffectiveText(), payload.getDeviceId());

        // Executa a regra de negócio se houver mensagem válida
        if (payload.getCleanPhoneNumber() != null && payload.getEffectiveText() != null) {
            whatsAppService.processIncomingWhatsAppMessage(payload);
        }

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Webhook processado com sucesso",
                "messageId", payload.getId()
        ));
    }
}
