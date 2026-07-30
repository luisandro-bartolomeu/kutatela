package ao.co.kutatelamama.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LocationServiceTest {

    private StubRestTemplate stubRestTemplate;
    private LocationService locationService;

    static class StubRestTemplate extends RestTemplate {
        boolean shouldThrowOnExchange = false;
        boolean shouldThrowOnPost = false;
        ResponseEntity<Map> exchangeResponse;
        ResponseEntity<Map> postResponse;

        @Override
        @SuppressWarnings("unchecked")
        public <T> ResponseEntity<T> exchange(URI url, HttpMethod method, HttpEntity<?> requestEntity, Class<T> responseType) throws RestClientException {
            if (shouldThrowOnExchange) {
                throw new RestClientException("504 Gateway Timeout");
            }
            return (ResponseEntity<T>) exchangeResponse;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> ResponseEntity<T> postForEntity(String url, Object request, Class<T> responseType, Object... uriVariables) throws RestClientException {
            if (shouldThrowOnPost) {
                throw new RestClientException("Connect timeout");
            }
            return (ResponseEntity<T>) postResponse;
        }
    }

    @BeforeEach
    void setUp() {
        stubRestTemplate = new StubRestTemplate();
        locationService = new LocationService(stubRestTemplate);
        ReflectionTestUtils.setField(locationService, "deepseekApiKey", "mock_key");
        ReflectionTestUtils.setField(locationService, "deepseekApiUrl", "https://api.deepseek.com/v1/chat/completions");
    }

    @Test
    @DisplayName("buscarHospitaisProximos retorna postos do OSM quando Overpass responde com sucesso")
    void testOsmSuccess() {
        Map<String, Object> osmResponseBody = new HashMap<>();
        Map<String, Object> element = new HashMap<>();
        element.put("lat", -8.8383);
        element.put("lon", 13.2344);
        element.put("tags", Map.of("name", "Hospital Geral de Luanda"));
        osmResponseBody.put("elements", List.of(element));

        stubRestTemplate.exchangeResponse = new ResponseEntity<>(osmResponseBody, HttpStatus.OK);

        String response = locationService.buscarHospitaisProximos(-8.8383, 13.2344);

        assertNotNull(response);
        assertTrue(response.contains("Hospital Geral de Luanda"));
        assertTrue(response.contains("Google Maps"));
    }

    @Test
    @DisplayName("buscarHospitaisProximos aciona fallback DeepSeek quando Overpass falha com excecao")
    void testOsmFailureTriggersDeepSeekFallback() {
        stubRestTemplate.shouldThrowOnExchange = true;

        String response = locationService.buscarHospitaisProximos(-8.8383, 13.2344);

        assertNotNull(response);
        assertTrue(response.contains("Mãe") || response.contains("Posto de Saúde") || response.contains("unidades de saúde"));
    }

    @Test
    @DisplayName("buscarHospitaisViaDeepSeek devolve mensagem amigavel fixa quando DeepSeek falha")
    void testDeepSeekFailureReturnsFriendlyMessage() {
        stubRestTemplate.shouldThrowOnPost = true;

        String response = locationService.buscarHospitaisViaDeepSeek(-8.8383, 13.2344);

        assertNotNull(response);
        assertTrue(response.contains("Mãe, de momento não foi possível obter as unidades de saúde"));
        assertTrue(response.contains("Posto de Saúde ou Maternidade Municipal"));
    }
}
