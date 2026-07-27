package ao.co.kutatelamama.controller;

import ao.co.kutatelamama.service.UssdSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/ussd")
@CrossOrigin(origins = "*")
public class UssdController {

    private static final Logger log = LoggerFactory.getLogger(UssdController.class);
    private final UssdSessionService ussdSessionService;

    public UssdController(UssdSessionService ussdSessionService) {
        this.ussdSessionService = ussdSessionService;
    }

    /**
     * Endpoint para Callback USSD do Africa's Talking (HTTP POST application/x-www-form-urlencoded)
     */
    @PostMapping(consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE, MediaType.ALL_VALUE}, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> handleUssdPost(@RequestParam Map<String, String> allParams) {
        String sessionId = getParam(allParams, "sessionId", "session_id", "default_session");
        String serviceCode = getParam(allParams, "serviceCode", "service_code", "*123#");
        String phoneNumber = getParam(allParams, "phoneNumber", "phone_number", "phone", "+244923000000");
        String text = getParam(allParams, "text", "Text", "TEXT", "");

        log.info("[USSD AT POST] sessionId: {}, serviceCode: {}, phone: {}, text: '{}'", sessionId, serviceCode, phoneNumber, text);

        String response = ussdSessionService.processUssdRequest(sessionId, serviceCode, phoneNumber, text);
        return ResponseEntity.ok(UssdSessionService.stripAccentsAndEmojis(response));
    }

    /**
     * Endpoint com payload JSON alternativo para testes
     */
    @PostMapping(path = "/json", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> handleUssdJson(@RequestBody Map<String, String> body) {
        String sessionId = getParam(body, "sessionId", "session_id", "session_" + System.currentTimeMillis());
        String serviceCode = getParam(body, "serviceCode", "service_code", "*123#");
        String phoneNumber = getParam(body, "phoneNumber", "phone_number", "phone", "+244923000000");
        String text = getParam(body, "text", "Text", "TEXT", "");

        String response = ussdSessionService.processUssdRequest(sessionId, serviceCode, phoneNumber, text);
        return ResponseEntity.ok(UssdSessionService.stripAccentsAndEmojis(response));
    }

    /**
     * Endpoint HTTP GET para testes no navegador
     */
    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> handleUssdGet(@RequestParam Map<String, String> allParams) {
        String sessionId = getParam(allParams, "sessionId", "session_id", "default_session");
        String serviceCode = getParam(allParams, "serviceCode", "service_code", "*123#");
        String phoneNumber = getParam(allParams, "phoneNumber", "phone_number", "phone", "+244923000000");
        String text = getParam(allParams, "text", "Text", "TEXT", "");

        String response = ussdSessionService.processUssdRequest(sessionId, serviceCode, phoneNumber, text);
        return ResponseEntity.ok(UssdSessionService.stripAccentsAndEmojis(response));
    }

    private String getParam(Map<String, String> params, String k1, String k2, String defaultValue) {
        if (params.containsKey(k1) && params.get(k1) != null) return params.get(k1);
        if (params.containsKey(k2) && params.get(k2) != null) return params.get(k2);
        return defaultValue;
    }

    private String getParam(Map<String, String> params, String k1, String k2, String k3, String defaultValue) {
        if (params.containsKey(k1) && params.get(k1) != null) return params.get(k1);
        if (params.containsKey(k2) && params.get(k2) != null) return params.get(k2);
        if (params.containsKey(k3) && params.get(k3) != null) return params.get(k3);
        return defaultValue;
    }
}
