package ao.co.kutatelamama.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.*;

@Service
public class LocationService {

    private static final Logger log = LoggerFactory.getLogger(LocationService.class);
    private static final String OVERPASS_API_URL = "https://overpass-api.de/api/interpreter";
    private static final String USER_AGENT = "KutatelaMamaHealthBot/1.0 (Angola Health Bot; contact@kutatelamama.ao)";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${deepseek.api.key:${DEEPSEEK_API_KEY:mock_key}}")
    private String deepseekApiKey;

    @Value("${deepseek.api.url:${DEEPSEEK_URL:https://api.deepseek.com/v1/chat/completions}}")
    private String deepseekApiUrl;

    public LocationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public static class HealthCenterItem {
        private final String name;
        private final double lat;
        private final double lon;
        private final double distanceKm;

        public HealthCenterItem(String name, double lat, double lon, double distanceKm) {
            this.name = name;
            this.lat = lat;
            this.lon = lon;
            this.distanceKm = distanceKm;
        }

        public String getName() { return name; }
        public double getLat() { return lat; }
        public double getLon() { return lon; }
        public double getDistanceKm() { return distanceKm; }
    }

    /**
     * Consulta a Overpass API do OpenStreetMap e formata a mensagem com os 3 postos/hospitais mais próximos.
     * Em caso de erro na Overpass API, aciona o Fallback inteligente via DeepSeek AI.
     */
    public String findNearestHealthCentersMessage(double userLat, double userLon) {
        return buscarHospitaisProximos(userLat, userLon);
    }

    /**
     * Fluxo principal: Busca os hospitais e maternidades mais próximos via OpenStreetMap (Overpass API).
     * Se falhar por qualquer motivo (429, 504, etc.), aciona o Fallback com a IA do DeepSeek.
     */
    public String buscarHospitaisProximos(double lat, double lon) {
        try {
            List<HealthCenterItem> centers = searchNearestHealthCenters(lat, lon);

            if (centers.isEmpty()) {
                log.warn("OpenStreetMap não retornou hospitais num raio de 7km. Acionando Fallback com DeepSeek...");
                return buscarHospitaisViaDeepSeek(lat, lon);
            }

            StringBuilder sb = new StringBuilder();
            sb.append("*Unidades de Saúde Mais Próximas de Si:*\n\n");

            int limit = Math.min(3, centers.size());
            for (int i = 0; i < limit; i++) {
                HealthCenterItem item = centers.get(i);
                String googleMapsUrl = String.format(Locale.US, "https://www.google.com/maps/dir/?api=1&destination=%.6f,%.6f", item.getLat(), item.getLon());

                sb.append(i + 1).append(". *").append(item.getName()).append("*\n")
                  .append("   Distância: ~").append(String.format(Locale.US, "%.1f", item.getDistanceKm())).append(" km\n")
                  .append("   Rota no Google Maps: ").append(googleMapsUrl).append("\n\n");
            }

            sb.append("*Dica:* Clique no link do Google Maps para iniciar a navegação até à unidade de saúde.");
            return sb.toString();

        } catch (Exception e) {
            log.warn("OpenStreetMap falhou ou foi bloqueado. Acionando Fallback com DeepSeek...");
            return buscarHospitaisViaDeepSeek(lat, lon);
        }
    }

    /**
     * Contingência via DeepSeek: Chamada HTTP POST para a API do DeepSeek (deepseek-chat)
     * quando a Overpass API falhar ou não retornar dados.
     */
    public String buscarHospitaisViaDeepSeek(double lat, double lon) {
        try {
            if (deepseekApiKey == null || deepseekApiKey.isBlank() || "mock_key".equalsIgnoreCase(deepseekApiKey.trim())) {
                log.warn("Chave da API do DeepSeek não configurada ou é mock_key. Devolvendo mensagem amigável de contingência.");
                return getFriendlyFallbackMessage();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(deepseekApiKey.trim());

            String systemPrompt = "Você é o assistente geográfico do projeto de saúde materno-infantil Kutatela Mama em Angola. " +
                    "Receba a latitude e longitude da mãe, identifique com precisão o município ou bairro correspondente na província de Luanda/Angola " +
                    "e liste os 3 principais hospitais ou maternidades públicas de referência mais próximos. " +
                    "Adicione links de rotas públicos do Google Maps usando os nomes dos hospitais no formato exato: google.com. Seja acolhedor e direto.";

            String userPrompt = String.format(Locale.US,
                    "Mãe enviou localização. Coordenadas: Latitude: %.6f, Longitude: %.6f. " +
                    "Identifique a região em Angola e liste os 3 hospitais públicos de referência com os links do Google Maps.",
                    lat, lon);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "deepseek-chat");
            requestBody.put("temperature", 0.2);

            List<Map<String, String>> messages = List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)
            );
            requestBody.put("messages", messages);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(deepseekApiUrl, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map body = response.getBody();
                List choices = (List) body.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map firstChoice = (Map) choices.get(0);
                    Map message = (Map) firstChoice.get("message");
                    String content = (String) message.get("content");
                    if (content != null && !content.isBlank()) {
                        log.info("DeepSeek Fallback respondeu com sucesso para lat={}, lon={}", lat, lon);
                        return content.trim();
                    }
                }
            }

            log.warn("DeepSeek API não devolveu conteúdo válido. Acionando resposta fixa amigável.");
            return getFriendlyFallbackMessage();

        } catch (Exception e) {
            log.error("Erro no Fallback do DeepSeek (saldo ou rede): {}", e.getMessage(), e);
            return getFriendlyFallbackMessage();
        }
    }

    private String getFriendlyFallbackMessage() {
        return "Mãe, de momento não foi possível obter as unidades de saúde em tempo real.\n" +
               "Por favor, dirija-se ao Posto de Saúde ou Maternidade Municipal mais próxima da sua residência para atendimento imediato.";
    }

    /**
     * Executa a query Overpass no OSM buscando hospital e maternidade num raio de 7000m.
     */
    @SuppressWarnings("unchecked")
    public List<HealthCenterItem> searchNearestHealthCenters(double userLat, double userLon) {
        String query = String.format(Locale.US,
                "[out:json][timeout:10];" +
                "(" +
                "  node[\"amenity\"=\"hospital\"](around:7000,%.6f,%.6f);" +
                "  node[\"name\"~\"Hospital|Maternidade\",i](around:7000,%.6f,%.6f);" +
                "  way[\"amenity\"=\"hospital\"](around:7000,%.6f,%.6f);" +
                "  way[\"name\"~\"Hospital|Maternidade\",i](around:7000,%.6f,%.6f);" +
                ");" +
                "out center tags;",
                userLat, userLon, userLat, userLon, userLat, userLon, userLat, userLon);

        URI uri = UriComponentsBuilder.fromHttpUrl(OVERPASS_API_URL)
                .queryParam("data", query)
                .build()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", USER_AGENT);

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(uri, HttpMethod.GET, requestEntity, Map.class);

        List<HealthCenterItem> list = new ArrayList<>();
        Set<String> addedKeys = new HashSet<>();

        if (response.getBody() != null && response.getBody().containsKey("elements")) {
            List<Map<String, Object>> elements = (List<Map<String, Object>>) response.getBody().get("elements");
            if (elements != null) {
                for (Map<String, Object> elem : elements) {
                    Double lat = parseDouble(elem.get("lat"));
                    Double lon = parseDouble(elem.get("lon"));

                    if ((lat == null || lon == null) && elem.containsKey("center") && elem.get("center") instanceof Map) {
                        Map<String, Object> center = (Map<String, Object>) elem.get("center");
                        lat = parseDouble(center.get("lat"));
                        lon = parseDouble(center.get("lon"));
                    }

                    if (lat != null && lon != null) {
                        String name = "Unidade de Saúde";
                        if (elem.containsKey("tags") && elem.get("tags") instanceof Map) {
                            Map<String, Object> tags = (Map<String, Object>) elem.get("tags");
                            if (tags.containsKey("name") && tags.get("name") != null) {
                                String tagName = tags.get("name").toString().trim();
                                if (!tagName.isEmpty()) {
                                    name = tagName;
                                }
                            } else if (tags.containsKey("amenity")) {
                                String amenity = tags.get("amenity").toString();
                                name = "hospital".equalsIgnoreCase(amenity) ? "Hospital" : "Centro de Saúde";
                            }
                        }

                        String nameKey = name.toLowerCase().trim();
                        String uniqueKey = ("Unidade de Saúde".equalsIgnoreCase(name) || "Hospital".equalsIgnoreCase(name) || "Centro de Saúde".equalsIgnoreCase(name))
                                ? nameKey + "@" + String.format(Locale.US, "%.3f,%.3f", lat, lon)
                                : nameKey;

                        if (!addedKeys.contains(uniqueKey)) {
                            addedKeys.add(uniqueKey);
                            double distance = calculateDistanceKm(userLat, userLon, lat, lon);
                            list.add(new HealthCenterItem(name, lat, lon, distance));
                        }
                    }
                }
            }
        }

        list.sort(Comparator.comparingDouble(HealthCenterItem::getDistanceKm));
        return list;
    }

    private Double parseDouble(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).doubleValue();
        try {
            return Double.parseDouble(val.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private double calculateDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
