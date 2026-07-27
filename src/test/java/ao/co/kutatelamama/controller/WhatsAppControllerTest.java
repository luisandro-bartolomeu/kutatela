package ao.co.kutatelamama.controller;

import ao.co.kutatelamama.dto.WhatsAppWebhookPayloadDto;
import ao.co.kutatelamama.service.WhatsAppService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class WhatsAppControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private boolean processed = false;

    @BeforeEach
    void setUp() {
        WhatsAppService stubService = new WhatsAppService(null, null) {
            @Override
            public void processIncomingWhatsAppMessage(WhatsAppWebhookPayloadDto payload) {
                processed = true;
            }
        };
        WhatsAppController controller = new WhatsAppController(stubService);
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
}
