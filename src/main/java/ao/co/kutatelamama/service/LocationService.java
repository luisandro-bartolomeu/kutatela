package ao.co.kutatelamama.service;

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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class LocationService {

    private static final Logger log = LoggerFactory.getLogger(LocationService.class);

    private static final List<String> OVERPASS_SERVERS = List.of(
        "https://overpass-api.de/api/interpreter",
        "https://overpass.kumi.systems/api/interpreter",
        "https://overpass.private.coffee/api/interpreter"
    );

    private static final String USER_AGENT = "KutatelaMamaHealthBot/1.0 (Angola Health Bot; contact@kutatelamama.ao)";

    private final RestTemplate restTemplate;

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
        private final String municipality;

        public HealthCenterItem(String name, double lat, double lon, double distanceKm, String municipality) {
            this.name = name;
            this.lat = lat;
            this.lon = lon;
            this.distanceKm = distanceKm;
            this.municipality = municipality;
        }

        public String getName() { return name; }
        public double getLat() { return lat; }
        public double getLon() { return lon; }
        public double getDistanceKm() { return distanceKm; }
        public String getMunicipality() { return municipality; }
    }

    // Base de dados local de contingência com unidades de saúde reais de Angola
    private static final List<HealthCenterItem> KNOWN_HEALTH_CENTERS = List.of(
        // Luanda - Camama / Kilamba Kiaxi / Viana
        new HealthCenterItem("Hospital Geral de Luanda", -8.8920, 13.2980, 0, "Camama / Kilamba Kiaxi"),
        new HealthCenterItem("Hospital Materno-Infantil Azancot de Menezes", -8.8985, 13.2921, 0, "Camama"),
        new HealthCenterItem("Centro de Saúde da Sapú", -8.8780, 13.3120, 0, "Kilamba Kiaxi / Sapú"),
        new HealthCenterItem("Hospital Municipal do Kilamba Kiaxi", -8.8710, 13.2950, 0, "Kilamba Kiaxi"),
        new HealthCenterItem("Hospital Municipal de Viana", -8.9050, 13.3750, 0, "Viana"),
        new HealthCenterItem("Centro de Saúde do Cazenga", -8.8150, 13.2850, 0, "Cazenga"),
        new HealthCenterItem("Maternidade Lucrécia Paim", -8.8250, 13.2350, 0, "Maianga / Luanda"),
        new HealthCenterItem("Hospital Josina Machel (Maria Pia)", -8.8120, 13.2310, 0, "Ingombota / Luanda"),
        new HealthCenterItem("Hospital Américo Boavida", -8.8280, 13.2590, 0, "Rangel / Luanda"),
        new HealthCenterItem("Hospital Municipal de Cacuaco", -8.7800, 13.3650, 0, "Cacuaco"),
        new HealthCenterItem("Hospital Municipal de Belas", -8.9650, 13.1850, 0, "Kilamba / Belas"),
        new HealthCenterItem("Centro de Saúde do Palanca", -8.8550, 13.2750, 0, "Kilamba Kiaxi"),

        // Benguela / Lobito
        new HealthCenterItem("Hospital Geral de Benguela", -12.5780, 13.4070, 0, "Benguela"),
        new HealthCenterItem("Hospital Geral do Lobito - Pediatria", -12.3550, 13.5450, 0, "Lobito"),
        new HealthCenterItem("Centro de Saúde da Caponte", -12.5830, 13.4120, 0, "Benguela"),

        // Huambo
        new HealthCenterItem("Hospital Geral do Huambo", -12.7750, 15.7390, 0, "Huambo"),
        new HealthCenterItem("Maternidade Central do Huambo", -12.7710, 15.7420, 0, "Huambo"),

        // Huíla
        new HealthCenterItem("Hospital Central do Lubango", -14.9170, 13.4930, 0, "Lubango"),
        new HealthCenterItem("Centro Materno-Infantil da Humpata", -15.0120, 13.3650, 0, "Humpata"),

        // Bié
        new HealthCenterItem("Hospital Provincial do Bié", -12.3830, 16.9450, 0, "Kuito"),

        // Uíge
        new HealthCenterItem("Hospital Geral do Uíge", -7.6080, 15.0610, 0, "Uíge"),

        // Cabinda
        new HealthCenterItem("Hospital Geral de Cabinda", -5.5560, 12.1920, 0, "Cabinda"),

        // Cuanza Sul
        new HealthCenterItem("Hospital Geral do Sumbe", -11.2050, 13.8420, 0, "Sumbe")
    );

    public String findNearestHealthCentersMessage(double userLat, double userLon) {
        return buscarHospitaisProximos(userLat, userLon);
    }

    public String buscarHospitaisProximos(double lat, double lon) {
        List<HealthCenterItem> centers = searchNearestHealthCentersOverpass(lat, lon);

        // Se OpenStreetMap não retornar resultados ou estiver instável, usa a base local de alta precisão
        if (centers.isEmpty()) {
            log.warn("[LOCATION] OpenStreetMap não retornou dados. Utilizando base local calibrada para Angola...");
            centers = getLocalHealthCenters(lat, lon);
        }

        if (!centers.isEmpty()) {
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
        }

        // Se nem a base local nem o OSM servirem, aciona a IA DeepSeek com contexto regional
        log.warn("[LOCATION] Acionando Fallback via IA DeepSeek com contexto geográfico...");
        return buscarHospitaisViaDeepSeek(lat, lon);
    }

    /**
     * Consulta servidores espelho da Overpass API com query expandida (hospitais, clínicas, centros e postos de saúde).
     */
    @SuppressWarnings("unchecked")
    private List<HealthCenterItem> searchNearestHealthCentersOverpass(double userLat, double userLon) {
        String query = String.format(Locale.US,
                "[out:json][timeout:8];" +
                "(" +
                "  node[\"amenity\"~\"hospital|clinic|doctors|health_post\"](around:10000,%.6f,%.6f);" +
                "  node[\"healthcare\"~\"hospital|clinic|centre\"](around:10000,%.6f,%.6f);" +
                "  node[\"name\"~\"Hospital|Maternidade|Centro|Posto|Saúde|Clínica|Clinica\",i](around:10000,%.6f,%.6f);" +
                "  way[\"amenity\"~\"hospital|clinic\"](around:10000,%.6f,%.6f);" +
                "  way[\"name\"~\"Hospital|Maternidade|Centro|Posto|Saúde|Clínica|Clinica\",i](around:10000,%.6f,%.6f);" +
                ");" +
                "out center tags;",
                userLat, userLon, userLat, userLon, userLat, userLon, userLat, userLon, userLat, userLon);

        for (String serverUrl : OVERPASS_SERVERS) {
            try {
                URI uri = UriComponentsBuilder.fromHttpUrl(serverUrl)
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
                                String uniqueKey = nameKey + "@" + String.format(Locale.US, "%.3f,%.3f", lat, lon);

                                if (!addedKeys.contains(uniqueKey)) {
                                    addedKeys.add(uniqueKey);
                                    double distance = calculateDistanceKm(userLat, userLon, lat, lon);
                                    list.add(new HealthCenterItem(name, lat, lon, distance, "Angola"));
                                }
                            }
                        }
                    }
                }

                if (!list.isEmpty()) {
                    list.sort(Comparator.comparingDouble(HealthCenterItem::getDistanceKm));
                    log.info("[LOCATION] OpenStreetMap respondeu com {} unidades de saúde via {}", list.size(), serverUrl);
                    return list;
                }
            } catch (Exception e) {
                log.warn("[LOCATION] Servidor Overpass {} falhou ({}), tentando próximo...", serverUrl, e.getMessage());
            }
        }
        return Collections.emptyList();
    }

    /**
     * Calcula as unidades de saúde reais mais próximas a partir da base local de Angola por fórmula Haversine.
     */
    private List<HealthCenterItem> getLocalHealthCenters(double userLat, double userLon) {
        List<HealthCenterItem> result = new ArrayList<>();
        for (HealthCenterItem item : KNOWN_HEALTH_CENTERS) {
            double dist = calculateDistanceKm(userLat, userLon, item.getLat(), item.getLon());
            result.add(new HealthCenterItem(item.getName(), item.getLat(), item.getLon(), dist, item.getMunicipality()));
        }
        result.sort(Comparator.comparingDouble(HealthCenterItem::getDistanceKm));
        return result;
    }

    /**
     * Fallback via DeepSeek AI com contexto geográfico preciso e URLs diretas do Google Maps.
     */
    public String buscarHospitaisViaDeepSeek(double lat, double lon) {
        try {
            if (deepseekApiKey == null || deepseekApiKey.isBlank() || "mock_key".equalsIgnoreCase(deepseekApiKey.trim())) {
                return getFriendlyFallbackMessage();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(deepseekApiKey.trim());

            String systemPrompt = "Você é um assistente de saúde para mães angolanas. Forneça APENAS os 3 hospitais ou centros de saúde públicos mais conhecidos da região indicada pelas coordenadas. Não faça cumprimentos longos.";

            String userPrompt = String.format(Locale.US,
                    "Localização da mãe em Angola: Latitude %.6f, Longitude %.6f.\n" +
                    "Responda estritamente no seguinte formato:\n" +
                    "*Unidades de Saúde Recomendadas:*\n\n" +
                    "1. *[Nome Real do Hospital/Centro de Saúde 1]*\n" +
                    "   Rota no Google Maps: https://www.google.com/maps/search/?api=1&query=[Nome+Hospital+1+Angola]\n\n" +
                    "2. *[Nome Real do Hospital/Centro de Saúde 2]*\n" +
                    "   Rota no Google Maps: https://www.google.com/maps/search/?api=1&query=[Nome+Hospital+2+Angola]\n\n" +
                    "3. *[Nome Real do Hospital/Centro de Saúde 3]*\n" +
                    "   Rota no Google Maps: https://www.google.com/maps/search/?api=1&query=[Nome+Hospital+3+Angola]",
                    lat, lon);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "deepseek-chat");
            requestBody.put("temperature", 0.1);

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
                        log.info("[LOCATION] DeepSeek Fallback respondeu com sucesso para lat={}, lon={}", lat, lon);
                        return content.trim();
                    }
                }
            }

            return getFriendlyFallbackMessage();

        } catch (Exception e) {
            log.error("[LOCATION] Erro no Fallback do DeepSeek: {}", e.getMessage());
            return getFriendlyFallbackMessage();
        }
    }

    private String getFriendlyFallbackMessage() {
        return "*Unidades de Saúde Mais Próximas:*\n\n" +
               "1. *Hospital Geral de Luanda (Camama)*\n" +
               "   Rota: https://www.google.com/maps/search/?api=1&query=Hospital+Geral+de+Luanda\n\n" +
               "2. *Hospital Materno-Infantil Azancot de Menezes*\n" +
               "   Rota: https://www.google.com/maps/search/?api=1&query=Hospital+Azancot+de+Menezes\n\n" +
               "3. *Maternidade Lucrécia Paim*\n" +
               "   Rota: https://www.google.com/maps/search/?api=1&query=Maternidade+Lucrecia+Paim\n\n" +
               "*Dica:* Clique nos links para abrir a navegação até ao posto de saúde.";
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
