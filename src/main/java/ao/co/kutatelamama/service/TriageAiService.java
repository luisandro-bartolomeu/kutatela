package ao.co.kutatelamama.service;

import ao.co.kutatelamama.domain.entity.Baby;
import ao.co.kutatelamama.domain.entity.Mother;
import ao.co.kutatelamama.domain.entity.TriageRecord;
import ao.co.kutatelamama.domain.enums.AlarmLevel;
import ao.co.kutatelamama.domain.enums.SymptomCategory;
import ao.co.kutatelamama.repository.TriageRecordRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TriageAiService {

    private static final Logger log = LoggerFactory.getLogger(TriageAiService.class);

    private final TriageRecordRepository triageRecordRepository;
    private final SmsService smsService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${kutatela.ai.deepseek-api-key:${DEEPSEEK_API_KEY:mock_key}}")
    private String deepseekApiKey;

    @Value("${kutatela.ai.deepseek-url:${DEEPSEEK_URL:https://api.deepseek.com/v1/chat/completions}}")
    private String deepseekUrl;

    public TriageAiService(TriageRecordRepository triageRecordRepository, SmsService smsService) {
        this.triageRecordRepository = triageRecordRepository;
        this.smsService = smsService;
    }

    public TriageRecord performTriage(Mother mother, Baby baby, SymptomCategory category, String detailInput) {
        log.info("[AI TRIAGE] Processing triage for mother: {}, baby: {}, category: {}, detail: {}",
                mother != null ? mother.getFullName() : "Anon",
                baby != null ? baby.getFullName() : "N/A",
                category, detailInput);

        TriageResult result = null;

        if (deepseekApiKey != null && !deepseekApiKey.isBlank() && !"mock_key".equalsIgnoreCase(deepseekApiKey)) {
            try {
                result = callDeepSeekAiApi(category, detailInput, baby != null ? baby.getAgeInMonths() : 1);
            } catch (Exception e) {
                log.warn("Falha ao chamar a API da DeepSeek AI, a utilizar motor clinico de contingencia: {}", e.getMessage());
            }
        }

        if (result == null) {
            result = processClinicalFallback(category, detailInput, baby != null ? baby.getAgeInMonths() : 1);
        }

        TriageRecord record = new TriageRecord(
                mother,
                baby,
                category,
                detailInput,
                result.analysis,
                result.homeCare,
                result.alarmSignals,
                result.healthCenterAdvice,
                result.alarmLevel
        );

        TriageRecord saved = triageRecordRepository.save(record);

        // Envio de resumo por SMS para a mãe
        if (mother != null && mother.getPhoneNumber() != null) {
            String smsText = String.format(
                "Kutatela Mama (Triagem): %s. Cuidados: %s. Nível: %s. Sinais de alarme: %s.",
                result.analysis,
                result.homeCare,
                result.alarmLevel.getTitle(),
                result.alarmSignals
            );
            if (smsText.length() > 320) {
                smsText = smsText.substring(0, 317) + "...";
            }
            smsService.sendSms(mother.getPhoneNumber(), "TRIAGE_SUMMARY", smsText);
        }

        return saved;
    }

    private TriageResult callDeepSeekAiApi(SymptomCategory category, String detailInput, long ageInMonths) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(deepseekApiKey.trim());

        String systemPrompt = "Você é a Dra. Kutatela, uma assistente médica de saúde materno-infantil em Angola (Manual do Mobilizador Social - MINSA 2018).\n" +
                "Sua missão é dar orientação médica preventiva simples, carinhosa, educativa e muito clara para mães angolanas, utilizando o conteúdo oficial do Ministério da Saúde de Angola.\n\n" +
                "REGRAS OBRIGATÓRIAS:\n" +
                "1. RECOMENDE SEMPRE que a mãe consulte um médico ou se dirija ao Posto/Centro de Saúde mais próximo.\n" +
                "2. Forneça conselhos práticos e seguros de primeiros socorros sobre o que a mãe deve fazer para remediar/cuidar do bebé AGORA, antes de ir ou no caminho até ao posto de saúde.\n" +
                "3. Conheça as vacinas do PNV Angola MINSA 2018 (BCG, Pólio, Hepatite B, Pentavalente, Pneumococo, Rotavírus, Sarampo/Rubéola, Febre Amarela).\n" +
                "4. Use linguagem extremamente simples, acolhedora e acessível.\n" +
                "5. Responda ESTRITAMENTE num formato JSON válido com as seguintes chaves:\n" +
                "{\n" +
                "  \"analysis\": \"Análise simples do sintoma\",\n" +
                "  \"homeCare\": \"Cuidados caseiros e primeiros socorros antes e a caminho do médico\",\n" +
                "  \"alarmSignals\": \"Sinais de perigo para atenção rápida\",\n" +
                "  \"healthCenterAdvice\": \"Recomendação obrigatória de consulta médica/posto de saúde\",\n" +
                "  \"alarmLevel\": \"NORMAL\" ou \"WARNING\" ou \"URGENT\"\n" +
                "}";

        String userPrompt = String.format("Bebé de %d meses de idade. Sintoma: %s. Detalhe fornecido: %s", ageInMonths, category, detailInput);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "deepseek-chat");
        requestBody.put("temperature", 0.2);
        requestBody.put("response_format", Map.of("type", "json_object"));

        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        );
        requestBody.put("messages", messages);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(deepseekUrl, entity, Map.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            Map body = response.getBody();
            List choices = (List) body.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map firstChoice = (Map) choices.get(0);
                Map message = (Map) firstChoice.get("message");
                String jsonContent = (String) message.get("content");

                try {
                    JsonNode node = objectMapper.readTree(jsonContent);

                    TriageResult res = new TriageResult();
                    res.analysis = node.has("analysis") ? node.get("analysis").asText() : "Avaliacao simples dos sintomas do bebe.";
                    res.homeCare = node.has("homeCare") ? node.get("homeCare").asText() : "Mantenha o bebe aquecido e amamentado no caminho para o posto de saude.";
                    res.alarmSignals = node.has("alarmSignals") ? node.get("alarmSignals").asText() : "Febre alta, prostracao ou dificuldade em respirar.";
                    res.healthCenterAdvice = node.has("healthCenterAdvice") ? node.get("healthCenterAdvice").asText() : "Procure SEMPRE o medico ou Posto de Saude mais proximo.";

                    String levelStr = node.has("alarmLevel") ? node.get("alarmLevel").asText() : "NORMAL";
                    try {
                        res.alarmLevel = AlarmLevel.valueOf(levelStr.toUpperCase());
                    } catch (Exception e) {
                        res.alarmLevel = AlarmLevel.NORMAL;
                    }

                    log.info("[DEEPSEEK AI] Triagem concluida com sucesso via API DeepSeek! Nivel: {}", res.alarmLevel);
                    return res;
                } catch (Exception e) {
                    log.error("Erro ao analisar resposta da DeepSeek API: {}", e.getMessage());
                    throw new RuntimeException("Erro de parse JSON da DeepSeek AI", e);
                }
            }
        }
        throw new RuntimeException("Resposta sem dados da API da DeepSeek");
    }

    private TriageResult processClinicalFallback(SymptomCategory category, String detailInput, long ageInMonths) {
        TriageResult res = new TriageResult();
        String detailLower = detailInput != null ? detailInput.toLowerCase() : "";

        switch (category) {
            case CHORO_PERSISTENTE:
                if (detailLower.contains("estridente") || detailLower.contains("gemido") || detailLower.contains("3") || detailLower.contains("4")) {
                    res.analysis = "Choro agudo ou com gemidos pode indicar dor intensa, febre ou infeção. O bebé precisa de atenção médica imediata.";
                    res.homeCare = "Acolha o bebé com carinho no regaço, verifique a roupa, meça a temperatura e mantenha-o aconchegado a caminho do posto.";
                    res.alarmSignals = "Choro persistente por mais de 3 horas, gemidos contínuos, febre associada, bebé mole/prostrado.";
                    res.healthCenterAdvice = "Vá IMEDIATAMENTE ao Posto de Saúde ou Hospital mais próximo para ser visto pelo médico.";
                    res.alarmLevel = AlarmLevel.URGENT;
                } else if (detailLower.contains("forte") || detailLower.contains("2")) {
                    res.analysis = "Choro forte e contínuo pode ser sinal de cólicas, fome ou fralda suja.";
                    res.homeCare = "Ofereça a mama, faça massagem suave na barriguinha no sentido dos ponteiros do relógio e flexione as perninhas suavemente antes de ir ao médico.";
                    res.alarmSignals = "Febre associada, vómitos em jato, recusa de mamar por mais de 6 horas.";
                    res.healthCenterAdvice = "Consulte o médico ou enfermeiro no Posto de Saúde se o choro não passar.";
                    res.alarmLevel = AlarmLevel.WARNING;
                } else {
                    res.analysis = "Choro fraco ou choramingado pode indicar cansaço, frio ou necessidade de aconchego.";
                    res.homeCare = "Ofereça a mama, verifique a temperatura e mantenha o corpinho morno no colo enquanto observa o bebé.";
                    res.alarmSignals = "Se o bebé ficar muito prostrado, sem forças para chorar ou recusar a mama.";
                    res.healthCenterAdvice = "Leve o bebé ao Posto de Saúde se mantiver recusa em mamar.";
                    res.alarmLevel = AlarmLevel.NORMAL;
                }
                break;

            case BORBULHAS_ERUPCOES:
                if (detailLower.contains("manchas") || detailLower.contains("febre") || detailLower.contains("2")) {
                    res.analysis = "Borbulhas ou manchas com febre podem indicar virose transmissível ou infeção.";
                    res.homeCare = "Amamente com frequência a caminho da unidade de saúde. Não aplique remédios ou pomadas caseiras desconhecidas na pele.";
                    res.alarmSignals = "Febre alta, manchinhas roxas, cansaço no peito, prostração.";
                    res.healthCenterAdvice = "Leve o bebé IMEDIATAMENTE ao Posto de Saúde para exame médico.";
                    res.alarmLevel = AlarmLevel.URGENT;
                } else if (detailLower.contains("assadura") || detailLower.contains("3")) {
                    res.analysis = "Assadura ou dermatite de fralda grave.";
                    res.homeCare = "Lave o bumbum com água morna e sabão neutro, seque com cuidado sem esfregar e deixe a pele apanhar ar antes de ir ao médico.";
                    res.alarmSignals = "Feridas abertas com pus, sangramento ou febre.";
                    res.healthCenterAdvice = "Procure o Posto de Saúde para o enfermeiro prescrever a pomada adequada.";
                    res.alarmLevel = AlarmLevel.WARNING;
                } else {
                    res.analysis = "Bolinhas vermelhas simples podem ser brotoeja (calor) ou irritação leve.";
                    res.homeCare = "Dê banho morno com água limpa, vista roupas leves de algodão e evite calor excessivo antes do exame médico.";
                    res.alarmSignals = "Se as borbulhas espalharem rapidamente ou causarem febre.";
                    res.healthCenterAdvice = "Mostre as borbulhas ao profissional de saúde no Posto de Saúde local.";
                    res.alarmLevel = AlarmLevel.NORMAL;
                }
                break;

            case FEBRE:
                if (detailLower.contains("alta") || detailLower.contains("estremecimento") || detailLower.contains("2") || detailLower.contains("3") || ageInMonths < 3) {
                    res.analysis = "Febre em bebé menor de 3 meses ou febre alta com corpo muito quente é um sinal de alerta urgente (possível malária ou infeção).";
                    res.homeCare = "Retire o excesso de agasalhos, passe pano com água morna (nunca fria) no corpo e amamente continuamente a caminho do médico.";
                    res.alarmSignals = "Convulsão, estremecimento de membros, recusa total em mamar, rigidez de nuca, prostração.";
                    res.healthCenterAdvice = "CORRA ao Posto de Saúde ou Hospital mais próximo para teste de malária e consulta médica urgente!";
                    res.alarmLevel = AlarmLevel.URGENT;
                } else {
                    res.analysis = "Febre baixa a moderada (37.5ºC a 38ºC). Pode ser reação vacinal ou infeção inicial.";
                    res.homeCare = "Amamente com frequência para evitar desidratação, desagasalhe o bebé e meça a temperatura antes de ir ao médico.";
                    res.alarmSignals = "Febre subir acima de 38.5ºC, durar mais de 24h ou surgirem manchas no corpo.";
                    res.healthCenterAdvice = "Vá ao Posto de Saúde para o médico avaliar a causa da febre.";
                    res.alarmLevel = AlarmLevel.WARNING;
                }
                break;

            case DIARREIA_VOMITOS:
                if (detailLower.contains("olhos fundos") || detailLower.contains("lágrimas") || detailLower.contains("3") || detailLower.contains("vómitos")) {
                    res.analysis = "Sinais de desidratação por diarreia/vómitos (olhos fundos, sem lágrimas). É perigoso em bebés!";
                    res.homeCare = "Ofereça leite materno continuadamente e SRO (Soro de Reidratação Oral) em colheradas no caminho para o médico.";
                    res.alarmSignals = "Bebé muito mole ou sonolento, recusa total de líquidos, fezes com sangue, vómitos repetidos.";
                    res.healthCenterAdvice = "Vá URGENTEMENTE ao Posto ou Centro de Saúde para hidratação e cuidados médicos.";
                    res.alarmLevel = AlarmLevel.URGENT;
                } else {
                    res.analysis = "Fezes mais moles ou golfadas esporádicas.";
                    res.homeCare = "Amamente com mais frequência e coloque o bebé em pé no ombro após mamar para arrotar antes da consulta.";
                    res.alarmSignals = "Diarreia com mais de 4 evacuações no dia, febre ou fraqueza no bebé.";
                    res.healthCenterAdvice = "Consulte o Posto de Saúde se a diarreia persistir.";
                    res.alarmLevel = AlarmLevel.WARNING;
                }
                break;

            case DIFICULDADE_MAMAR:
                if (detailLower.contains("rejeita") || detailLower.contains("pega fraca") || detailLower.contains("1") || detailLower.contains("2")) {
                    res.analysis = "Dificuldade na pega ou recusa de mamar (pode ser nariz entupido ou febre).";
                    res.homeCare = "Limpe o narizinho com soro, certifique-se que o lábio do bebé abrange a maior parte da aréola e tente mamadas curtas antes de ir ao posto.";
                    res.alarmSignals = "Bebé sem urinar há mais de 8 horas, fraqueza extrema, recusa total em mamar.";
                    res.healthCenterAdvice = "Visite o Posto de Saúde para o enfermeiro/médico ajudar a ajustar a pega e examinar o bebé.";
                    res.alarmLevel = AlarmLevel.WARNING;
                } else {
                    res.analysis = "Dor no peito da mãe ou seios empedrados (ingurgitamento).";
                    res.homeCare = "Aplique pano morno no peito antes de amamentar, faça massagem circular e ordenhe um pouco de leite para aliviar antes do posto.";
                    res.alarmSignals = "Febre alta na mãe, vermelhidão intensa num seio com calafrios.";
                    res.healthCenterAdvice = "Dirija-se ao Posto de Saúde ou Maternidade para assistência médica materna.";
                    res.alarmLevel = AlarmLevel.WARNING;
                }
                break;

            default:
                res.analysis = "Avaliação de sintomas gerais do bebé.";
                res.homeCare = "Mantenha o bebé limpo, aquecido e bem amamentado no caminho até ao posto de saúde.";
                res.alarmSignals = "Dificuldade em respirar, febre alta, prostração, pele/olhos amarelos.";
                res.healthCenterAdvice = "Procure SEMPRE o médico ou Posto de Saúde mais próximo para consulta.";
                res.alarmLevel = AlarmLevel.NORMAL;
                break;
        }

        return res;
    }

    private TriageResult callExternalAiApi(SymptomCategory category, String detailInput, long ageInMonths) {
        return callDeepSeekAiApi(category, detailInput, ageInMonths);
    }

    public static class TriageResult {
        public String analysis;
        public String homeCare;
        public String alarmSignals;
        public String healthCenterAdvice;
        public AlarmLevel alarmLevel = AlarmLevel.NORMAL;
    }
}

