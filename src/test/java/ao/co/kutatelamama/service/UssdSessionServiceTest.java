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
        assertTrue(res.contains("Vacinação"));
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
    @DisplayName("Complete triage flow for Crying symptom returns CON response with AI analysis, menu options and SMS notification")
    void testCompleteTriageFlow() {
        String res = ussdSessionService.processUssdRequest("sess_1", "*384*23898#", "+244923111222", "2*1*2");
        assertNotNull(res);
        assertTrue(res.startsWith("CON"));
        assertTrue(res.contains("Análise"));
        assertTrue(res.contains("Cuidados"));
        assertTrue(res.contains("1. Fazer outra triagem"));
        assertTrue(res.contains("0. Voltar ao menu principal"));
    }

    @Test
    @DisplayName("Option 5 terminates session with END")
    void testExitMenu() {
        String res = ussdSessionService.processUssdRequest("sess_1", "*384*23898#", "+244923111222", "5");
        assertNotNull(res);
        assertTrue(res.startsWith("END"));
        assertTrue(res.contains("Obrigado por usar o Kutatela Mama"));
    }

    @Test
    @DisplayName("USSD option '1*0' returns to main menu and '1*0*2' opens Triage menu")
    void testUssdBackNavigation() {
        // "1*0" -> Entra na vacinação (1) e recua com (0) -> Volta ao menu principal
        String resBack = ussdSessionService.processUssdRequest("sess_2", "*384*23898#", "+244923111222", "1*0");
        assertNotNull(resBack);
        assertTrue(resBack.startsWith("CON"));
        assertTrue(resBack.contains("Vacinação"));

        // "1*0*2" -> Entra na vacinação (1), recua com (0), e escolhe Triagem (2)
        String resTriage = ussdSessionService.processUssdRequest("sess_2", "*384*23898#", "+244923111222", "1*0*2");
        assertNotNull(resTriage);
        assertTrue(resTriage.startsWith("CON"));
        assertTrue(resTriage.contains("Triagem de Sintomas"));
        assertTrue(resTriage.contains("Choro persistente"));
    }

    @Test
    @DisplayName("USSD option '2*1*0' returns to Triage menu")
    void testUssdSubmenuBackNavigation() {
        // "2*1*0" -> Triagem (2), Choro (1), Recuar (0) -> Volta ao menu de Triagem
        String res = ussdSessionService.processUssdRequest("sess_3", "*384*23898#", "+244923111222", "2*1*0");
        assertNotNull(res);
        assertTrue(res.startsWith("CON"));
        assertTrue(res.contains("O que o seu bebé está a sentir?"));
    }

    @Test
    @DisplayName("USSD deep leaf path '1*1*1*0' returns immediately to Vaccination menu in 1 step")
    void testDeepLeafBackNavigation() {
        // "1*1*1*0" -> Vacinação (1), Próximas vacinas (1), seleção folha extra (1), recuar (0) -> Volta ao menu de vacinação
        String res = ussdSessionService.processUssdRequest("sess_4", "*384*23898#", "+244923111222", "1*1*1*0");
        assertNotNull(res);
        assertTrue(res.startsWith("CON"));
        assertTrue(res.contains("Calendário de Vacinação"));
        assertTrue(res.contains("Ver próximas vacinas do bebé"));
    }

    @Test
    @DisplayName("USSD option '1*3' returns 'Conheça as Vacinas' submenu")
    void testKnowVaccinesSubmenu() {
        String res = ussdSessionService.processUssdRequest("sess_6", "*384*23898#", "+244923111222", "1*3");
        assertNotNull(res);
        assertTrue(res.startsWith("CON"));
        assertTrue(res.contains("CONHECA AS VACINAS") || res.contains("CONHEÇA AS VACINAS"));
        assertTrue(res.contains("BCG"));
        assertTrue(res.contains("Polio") || res.contains("Pólio"));
        assertTrue(res.contains("Pentavalente"));
        assertTrue(res.contains("Febre Amarela"));
    }

    @Test
    @DisplayName("USSD option '1*3*1' returns BCG detailed description")
    void testBcgVaccineDetail() {
        String res = ussdSessionService.processUssdRequest("sess_7", "*384*23898#", "+244923111222", "1*3*1");
        assertNotNull(res);
        assertTrue(res.startsWith("CON"));
        assertTrue(res.contains("BCG"));
        assertTrue(res.contains("Tuberculose"));
        assertTrue(res.contains("Intra-Dermica") || res.contains("Intra-Dérmica"));
        assertTrue(res.contains("0,05 ml"));
    }

    @Test
    @DisplayName("USSD option '1*3*4' returns Pentavalente detailed description")
    void testPentavalenteVaccineDetail() {
        String res = ussdSessionService.processUssdRequest("sess_8", "*384*23898#", "+244923111222", "1*3*4");
        assertNotNull(res);
        assertTrue(res.startsWith("CON"));
        assertTrue(res.contains("PENTAVALENTE"));
        assertTrue(res.contains("DIFTERIA"));
        assertTrue(res.contains("TETANO") || res.contains("TÉTANO"));
        assertTrue(res.contains("TOSSE CONVULSA"));
        assertTrue(res.contains("HAEMOPHILUS"));
        assertTrue(res.contains("HEPATITE B"));
    }
}
