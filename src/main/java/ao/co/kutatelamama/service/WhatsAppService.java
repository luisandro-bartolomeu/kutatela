package ao.co.kutatelamama.service;

import ao.co.kutatelamama.dto.WhatsAppWebhookPayloadDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WhatsAppService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppService.class);

    private final RestTemplate restTemplate;
    private final UssdSessionService ussdSessionService;

    private static class WhatsAppSession {
        private final String sessionText;
        private final long lastActivityTimestamp;

        public WhatsAppSession(String sessionText, long lastActivityTimestamp) {
            this.sessionText = sessionText;
            this.lastActivityTimestamp = lastActivityTimestamp;
        }

        public String getSessionText() { return sessionText; }
        public long getLastActivityTimestamp() { return lastActivityTimestamp; }
    }

    // Memória temporária para acumular o estado do menu e timestamp por número de telefone (Timeout: 10 minutos)
    private final Map<String, WhatsAppSession> sessionCache = new ConcurrentHashMap<>();
    private static final long SESSION_TIMEOUT_MS = 10 * 60 * 1000L; // 10 minutos em ms

    @Value("${kutatela.gowa.api-url:${gowa.api.url:https://go-whatsapp-web-multidevice-production-4328.up.railway.app}}")
    private String gowaBaseUrl;

    @Value("${kutatela.gowa.device-id:${gowa.api.default-device-id:kutatela_mama}}")
    private String defaultDeviceId;

    public WhatsAppService(RestTemplate restTemplate, UssdSessionService ussdSessionService) {
        this.restTemplate = restTemplate;
        this.ussdSessionService = ussdSessionService;
    }

    /**
     * Captura a mensagem do WhatsApp, reconstrói o estado do menu e chama o motor do menu.
     */
    public void processIncomingWhatsAppMessage(WhatsAppWebhookPayloadDto payload) {
        if (payload == null || payload.isGroupMessage()) {
            log.info("[WHATSAPP] Mensagem nula ou de grupo ignorada");
            return;
        }

        String rawPhone = payload.getCleanPhoneNumber();
        if (rawPhone == null || rawPhone.isBlank()) {
            return;
        }

        String fromPhone = rawPhone.split("@")[0].trim();
        if (!fromPhone.startsWith("+") && fromPhone.matches("\\d+")) {
            fromPhone = "+" + fromPhone;
        }

        String receivedText = payload.getEffectiveText() != null ? payload.getEffectiveText().trim() : "";

        String activeDeviceId = (payload.getDeviceId() != null && !payload.getDeviceId().isBlank())
                ? payload.getDeviceId()
                : defaultDeviceId;

        long now = System.currentTimeMillis();
        WhatsAppSession existingSession = sessionCache.get(fromPhone);
        String currentSessionText = "";

        if (existingSession != null) {
            long elapsedTime = now - existingSession.getLastActivityTimestamp();
            if (elapsedTime > SESSION_TIMEOUT_MS) {
                log.info("[WHATSAPP TIMEOUT] Sessao de '{}' expirou apos {}ms (>10 min de inatividade). Reiniciando conversa no menu principal.", fromPhone, elapsedTime);
                sessionCache.remove(fromPhone);
                currentSessionText = "";
            } else {
                currentSessionText = existingSession.getSessionText();
            }
        }

        // Proteção inicial se a mãe mandar texto solto sem sessão ativa
        if (currentSessionText.isEmpty() && !receivedText.matches("[1-5]") && !receivedText.equalsIgnoreCase("menu") && !receivedText.equalsIgnoreCase("00")) {
            receivedText = "";
        }

        // Construção da sequência de entrada
        String inputSequence;
        if (receivedText.equalsIgnoreCase("menu") || receivedText.equalsIgnoreCase("00")) {
            inputSequence = "";
            sessionCache.remove(fromPhone);
        } else if (currentSessionText.isEmpty()) {
            inputSequence = receivedText;
        } else if (receivedText.isEmpty()) {
            inputSequence = currentSessionText;
        } else {
            inputSequence = currentSessionText + "*" + receivedText;
        }

        // Utiliza o redutor de stack do USSD
        String[] cleanTokens = ussdSessionService.cleanAndReduceUssdPath(inputSequence);
        String reducedPath = String.join("*", cleanTokens);

        log.info("[MENU WHATSAPP] Numero: '{}' | Entrada: '{}' -> Estado Reduzido: '{}'", fromPhone, inputSequence, reducedPath);

        // Executa o motor de menus USSD com a string de navegação corrigida
        String rawMenuResponse = ussdSessionService.processUssdRequest(fromPhone, "*404#", fromPhone, reducedPath);

        // Limpeza dos prefixos CON e END
        String finalWhatsAppMessage = rawMenuResponse;
        boolean isContinuation = false;

        if (finalWhatsAppMessage.startsWith("CON ")) {
            finalWhatsAppMessage = finalWhatsAppMessage.replace("CON ", "");
            isContinuation = true;
            sessionCache.put(fromPhone, new WhatsAppSession(reducedPath, System.currentTimeMillis())); // Grava estado e timestamp
        } else if (finalWhatsAppMessage.startsWith("END ")) {
            finalWhatsAppMessage = finalWhatsAppMessage.replace("END ", "");
            sessionCache.remove(fromPhone);
        }

        if (isContinuation && !finalWhatsAppMessage.contains("0. Voltar")) {
            finalWhatsAppMessage += "\n\nEnvie 0 para voltar ao menu principal.";
        }

        sendWhatsAppMessage(activeDeviceId, fromPhone, finalWhatsAppMessage);
    }

    /**
     * Realiza a requisição POST para o GoWA enviando o texto e injetando os cabeçalhos.
     */
    public boolean sendWhatsAppMessage(String deviceId, String phone, String text) {
        String baseUrl = gowaBaseUrl.endsWith("/") ? gowaBaseUrl.substring(0, gowaBaseUrl.length() - 1) : gowaBaseUrl;
        String url = baseUrl + "/send/message";
        String targetDeviceId = (deviceId != null && !deviceId.isBlank()) ? deviceId : defaultDeviceId;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Device-Id", targetDeviceId);

            Map<String, Object> body = new HashMap<>();
            body.put("phone", phone);
            body.put("to", phone);
            body.put("receiver", phone);
            body.put("message", text);
            body.put("body", text);
            body.put("text", text);
            body.put("device_id", targetDeviceId);
            body.put("session_id", targetDeviceId);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            restTemplate.postForEntity(url, requestEntity, String.class);

            log.info("Mensagem enviada via GoWA para {} (DeviceId: {})!", phone, targetDeviceId);
            return true;
        } catch (Exception e) {
            log.error("Erro ao comunicar com a API do GoWA ({}) para {}: {}", url, phone, e.getMessage());
            return false;
        }
    }
}
