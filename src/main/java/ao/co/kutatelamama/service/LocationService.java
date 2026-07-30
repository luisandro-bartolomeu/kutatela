package ao.co.kutatelamama.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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
     * Consulta a Overpass API do OpenStreetMap e formata a mensagem com os 3 postos/hospitais mais proximos.
     */
    public String findNearestHealthCentersMessage(double userLat, double userLon) {
        try {
            List<HealthCenterItem> centers = buscarHospitaisProximos(userLat, userLon);

            if (centers.isEmpty()) {
                return "📍 Mãe, não encontramos postos ou clínicas de saúde num raio de 10km da sua localização atual.\n\n" +
                       "Recomendamos dirigir-se à unidade de saúde mais próxima do seu município ou contactar os serviços locais.";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("📍 *Unidades de Saúde Mais Próximas de Si:*\n\n");

            int limit = Math.min(3, centers.size());
            for (int i = 0; i < limit; i++) {
                HealthCenterItem item = centers.get(i);
                String googleMapsUrl = String.format(Locale.US, "https://www.google.com/maps/dir/?api=1&destination=%.6f,%.6f", item.getLat(), item.getLon());

                sb.append(i + 1).append(". *").append(item.getName()).append("*\n")
                  .append("   📏 Distância: ~").append(String.format(Locale.US, "%.1f", item.getDistanceKm())).append(" km\n")
                  .append("   🧭 Rota no Google Maps: ").append(googleMapsUrl).append("\n\n");
            }

            sb.append("💡 *Dica:* Clique no link do Google Maps para iniciar a navegação até ao posto de vacinação.");
            return sb.toString();

        } catch (Exception e) {
            log.error("Erro ao consultar Overpass API do OpenStreetMap para lat={}, lon={}: {}", userLat, userLon, e.getMessage(), e);
            return "📍 Não foi possível obter as unidades de saúde em tempo real de momento.\n" +
                   "Por favor, tente novamente mais tarde ou consulte o posto de saúde do seu município.";
        }
    }

    /**
     * Busca os hospitais e clinicas mais proximos num raio de 10km.
     */
    public List<HealthCenterItem> buscarHospitaisProximos(double userLat, double userLon) {
        return searchNearestHealthCenters(userLat, userLon);
    }

    /**
     * Executa a query Overpass no OSM buscando hospital e clinic num raio de 10000m.
     * Utiliza java.net.URI para evitar dupla codificacao (Double URL Encoding) no RestTemplate.
     */
    @SuppressWarnings("unchecked")
    public List<HealthCenterItem> searchNearestHealthCenters(double userLat, double userLon) {
        String query = String.format(Locale.US,
                "[out:json][timeout:25];(node[\"amenity\"~\"hospital|clinic\"](around:10000,%.6f,%.6f);way[\"amenity\"~\"hospital|clinic\"](around:10000,%.6f,%.6f););out center tags;",
                userLat, userLon, userLat, userLon);

        // Constroi java.net.URI pre-formatado para evitar re-encoding pelo RestTemplate
        URI uri = UriComponentsBuilder.fromHttpUrl(OVERPASS_API_URL)
                .queryParam("data", query)
                .build()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", USER_AGENT);

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(uri, HttpMethod.GET, requestEntity, Map.class);

        List<HealthCenterItem> list = new ArrayList<>();
        if (response.getBody() != null && response.getBody().containsKey("elements")) {
            List<Map<String, Object>> elements = (List<Map<String, Object>>) response.getBody().get("elements");
            if (elements != null) {
                for (Map<String, Object> elem : elements) {
                    Double lat = parseDouble(elem.get("lat"));
                    Double lon = parseDouble(elem.get("lon"));

                    // Se for um 'way' ou 'relation', extrai a latitude e longitude de dentro do no 'center'
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

                        double distance = calculateDistanceKm(userLat, userLon, lat, lon);
                        list.add(new HealthCenterItem(name, lat, lon, distance));
                    }
                }
            }
        }

        // Ordena pela menor distancia em km
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
        final int R = 6371; // Raio da Terra em km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
