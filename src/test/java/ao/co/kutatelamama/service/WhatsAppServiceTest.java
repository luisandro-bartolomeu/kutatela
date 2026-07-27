package ao.co.kutatelamama.service;

import ao.co.kutatelamama.dto.WhatsAppWebhookPayloadDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
class WhatsAppServiceTest {

    @Autowired
    private WhatsAppService whatsAppService;

    @Test
    @DisplayName("Processa mensagem 'MENU' sem lançar exceções")
    void testProcessMenuMessage() {
        WhatsAppWebhookPayloadDto payload = new WhatsAppWebhookPayloadDto(
                "msg_1",
                "+244923888777",
                "MENU",
                "MENU",
                "+244923888777",
                "Ana Paula"
        );

        assertDoesNotThrow(() -> whatsAppService.processIncomingWhatsAppMessage(payload));
    }

    @Test
    @DisplayName("Processa mensagem de vacinação '1'")
    void testProcessVaccinationMenuMessage() {
        WhatsAppWebhookPayloadDto payload = new WhatsAppWebhookPayloadDto(
                "msg_2",
                "+244923888777",
                "1",
                "1",
                "+244923888777",
                "Ana Paula"
        );

        assertDoesNotThrow(() -> whatsAppService.processIncomingWhatsAppMessage(payload));
    }

    @Test
    @DisplayName("Processa sintoma de texto livre com IA")
    void testProcessFreeFormSymptomMessage() {
        WhatsAppWebhookPayloadDto payload = new WhatsAppWebhookPayloadDto(
                "msg_3",
                "+244923888777",
                "O meu bebé tem febre de 38.5 graus e diarreia",
                "O meu bebé tem febre de 38.5 graus e diarreia",
                "+244923888777",
                "Ana Paula"
        );

        assertDoesNotThrow(() -> whatsAppService.processIncomingWhatsAppMessage(payload));
    }
}
