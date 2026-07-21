package ao.co.kutatelamama.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UssdSessionServiceTest {

    @Autowired
    private UssdSessionService ussdSessionService;

    @Test
    @DisplayName("Root USSD request returns CON main menu")
    void testRootMenu() {
        String res = ussdSessionService.processUssdRequest("sess_1", "*384*23898#", "+244923111222", "");
        assertNotNull(res);
        assertTrue(res.startsWith("CON"));
        assertTrue(res.contains("Calendário de Vacinação"));
        assertTrue(res.contains("Triagem de Sintomas"));
    }

    @Test
    @DisplayName("Option 1 returns Vaccination menu")
    void testVaccinationMenu() {
        String res = ussdSessionService.processUssdRequest("sess_1", "*384*23898#", "+244923111222", "1");
        assertNotNull(res);
        assertTrue(res.startsWith("CON"));
        assertTrue(res.contains("Ver próximas vacinas"));
    }

    @Test
    @DisplayName("Option 2 returns Symptom Triage menu")
    void testTriageMenu() {
        String res = ussdSessionService.processUssdRequest("sess_1", "*384*23898#", "+244923111222", "2");
        assertNotNull(res);
        assertTrue(res.startsWith("CON"));
        assertTrue(res.contains("Choro persistente"));
    }

    @Test
    @DisplayName("Complete triage flow for Crying symptom returns END response with AI analysis and SMS notification")
    void testCompleteTriageFlow() {
        String res = ussdSessionService.processUssdRequest("sess_1", "*384*23898#", "+244923111222", "2*1*2");
        assertNotNull(res);
        assertTrue(res.startsWith("END"));
        assertTrue(res.contains("Análise"));
        assertTrue(res.contains("Cuidados"));
    }

    @Test
    @DisplayName("Option 5 terminates session with END")
    void testExitMenu() {
        String res = ussdSessionService.processUssdRequest("sess_1", "*384*23898#", "+244923111222", "5");
        assertNotNull(res);
        assertTrue(res.startsWith("END"));
        assertTrue(res.contains("Obrigado por usar o Kutatela Mama"));
    }
}
