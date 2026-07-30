package ao.co.kutatelamama.controller;

import ao.co.kutatelamama.dto.WhatsAppWebhookPayloadDto;
import ao.co.kutatelamama.service.LocationService;
import ao.co.kutatelamama.service.WhatsAppService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class WhatsAppControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private boolean processed = false;
    private boolean locationProcessed = false;

    @BeforeEach
    void setUp() {
        WhatsAppService stubService = new WhatsAppService(null, null) {
            @Override
            public void processIncomingWhatsAppMessage(WhatsAppWebhookPayloadDto payload) {
                processed = true;
            }

            @Override
            public boolean sendWhatsAppMessage(String deviceId, String phone, String text) {
                return true;
            }
        };

        LocationService stubLocationService = new LocationService(null) {
            @Override
            public String findNearestHealthCentersMessage(double userLat, double userLon) {
                locationProcessed = true;
                return "📍 Unidades de Saúde Encontradas";
            }
        };

        WhatsAppController controller = new WhatsAppController(stubService, stubLocationService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("POST /api/v1/whatsapp/webhook processa payload do Gowa e retorna 200 OK")
    void testHandleWhatsAppWebhook() throws Exception {
        WhatsAppWebhookPayloadDto payload = new WhatsAppWebhookPayloadDto(
                "msg_12345",
                "244923111222@s.whatsapp.net",
                "1",
                "1",
                "+244923111222",
                "Maria Chitumba"
        );

        mockMvc.perform(post("/api/v1/whatsapp/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.messageId").value("msg_12345"));

        assert processed;
    }

    @Test
    @DisplayName("POST /api/v1/whatsapp/webhook processa mensagem de localização")
    void testHandleWhatsAppLocationWebhook() throws Exception {
        WhatsAppWebhookPayloadDto payload = new WhatsAppWebhookPayloadDto();
        payload.setId("msg_loc_123");
        payload.setFrom("+244923111222");
        payload.setType("location");
        payload.setPayload(Map.of(
                "latitude", -8.8383,
                "longitude", 13.2344
        ));

        mockMvc.perform(post("/api/v1/whatsapp/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        assert locationProcessed;
    }
}
