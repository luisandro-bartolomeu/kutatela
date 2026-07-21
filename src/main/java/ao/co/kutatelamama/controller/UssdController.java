package ao.co.kutatelamama.controller;

import ao.co.kutatelamama.service.UssdSessionService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/ussd")
@CrossOrigin(origins = "*")
public class UssdController {

    private final UssdSessionService ussdSessionService;

    public UssdController(UssdSessionService ussdSessionService) {
        this.ussdSessionService = ussdSessionService;
    }

    /**
     * Endpoint for Africa's Talking USSD Callback (HTTP POST application/x-www-form-urlencoded)
     */
    @PostMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> handleUssdPost(
            @RequestParam(value = "sessionId", required = false, defaultValue = "default_session") String sessionId,
            @RequestParam(value = "serviceCode", required = false, defaultValue = "*123#") String serviceCode,
            @RequestParam(value = "phoneNumber", required = false, defaultValue = "+244923000000") String phoneNumber,
            @RequestParam(value = "text", required = false, defaultValue = "") String text) {

        String response = ussdSessionService.processUssdRequest(sessionId, serviceCode, phoneNumber, text);
        return ResponseEntity.ok(response);
    }

    /**
     * JSON payload endpoint alternative for test clients
     */
    @PostMapping(path = "/json", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> handleUssdJson(@RequestBody Map<String, String> body) {
        String sessionId = body.getOrDefault("sessionId", "session_" + System.currentTimeMillis());
        String serviceCode = body.getOrDefault("serviceCode", "*123#");
        String phoneNumber = body.getOrDefault("phoneNumber", "+244923000000");
        String text = body.getOrDefault("text", "");

        String response = ussdSessionService.processUssdRequest(sessionId, serviceCode, phoneNumber, text);
        return ResponseEntity.ok(response);
    }

    /**
     * HTTP GET endpoint for quick browser testing
     */
    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> handleUssdGet(
            @RequestParam(value = "sessionId", required = false, defaultValue = "default_session") String sessionId,
            @RequestParam(value = "serviceCode", required = false, defaultValue = "*123#") String serviceCode,
            @RequestParam(value = "phoneNumber", required = false, defaultValue = "+244923000000") String phoneNumber,
            @RequestParam(value = "text", required = false, defaultValue = "") String text) {

        String response = ussdSessionService.processUssdRequest(sessionId, serviceCode, phoneNumber, text);
        return ResponseEntity.ok(response);
    }
}
