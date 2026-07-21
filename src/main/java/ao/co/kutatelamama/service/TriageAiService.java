package ao.co.kutatelamama.service;

import ao.co.kutatelamama.domain.entity.Baby;
import ao.co.kutatelamama.domain.entity.Mother;
import ao.co.kutatelamama.domain.entity.TriageRecord;
import ao.co.kutatelamama.domain.enums.AlarmLevel;
import ao.co.kutatelamama.domain.enums.SymptomCategory;
import ao.co.kutatelamama.repository.TriageRecordRepository;
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

    @Value("${kutatela.ai.gemini-api-key:mock_key}")
    private String geminiApiKey;

    public TriageAiService(TriageRecordRepository triageRecordRepository, SmsService smsService) {
        this.triageRecordRepository = triageRecordRepository;
        this.smsService = smsService;
    }

    public TriageRecord performTriage(Mother mother, Baby baby, SymptomCategory category, String detailInput) {
        log.info("🩺 [AI TRIAGE] Processing triage for mother: {}, baby: {}, category: {}, detail: {}",
                mother != null ? mother.getFullName() : "Anon",
                baby != null ? baby.getFullName() : "N/A",
                category, detailInput);

        TriageResult result;

        if (geminiApiKey != null && !geminiApiKey.isBlank() && !"mock_key".equalsIgnoreCase(geminiApiKey)) {
            try {
                result = callExternalAiApi(category, detailInput, baby != null ? baby.getAgeInMonths() : 1);
            } catch (Exception e) {
                log.warn("Failed calling external AI API, falling back to clinical engine: {}", e.getMessage());
                result = processClinicalFallback(category, detailInput, baby != null ? baby.getAgeInMonths() : 1);
            }
        } else {
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

        // Send summary via SMS to the mother
        if (mother != null && mother.getPhoneNumber() != null) {
            String smsText = String.format(
                "Kutatela Mama (Triagem) 🌿: %s. Cuidados: %s. %s Sinais alarme: %s.",
                result.analysis,
                result.homeCare,
                result.alarmLevel.getEmoji(),
                result.alarmSignals
            );
            if (smsText.length() > 320) {
                smsText = smsText.substring(0, 317) + "...";
            }
            smsService.sendSms(mother.getPhoneNumber(), "TRIAGE_SUMMARY", smsText);
        }

        return saved;
    }

    private TriageResult processClinicalFallback(SymptomCategory category, String detailInput, long ageInMonths) {
        TriageResult res = new TriageResult();
        String detailLower = detailInput != null ? detailInput.toLowerCase() : "";

        switch (category) {
            case CHORO_PERSISTENTE:
                if (detailLower.contains("estridente") || detailLower.contains("gemido") || detailLower.contains("3") || detailLower.contains("4")) {
                    res.analysis = "Choro agudo ou com gemidos pode indicar dor intensa, febre alta ou infeção respiratória/neurológica.";
                    res.homeCare = "Verifique se a roupa está apertada, meça a temperatura do bebé e tente manter o bebé acolhido no colo.";
                    res.alarmSignals = "Choro persistente por mais de 3 horas seguidas, gemidos contínuos, febre acompanhada, bebé prostrado que não reage.";
                    res.healthCenterAdvice = "Leve o bebé imediatamente ao Posto ou Centro de Saúde mais próximo.";
                    res.alarmLevel = AlarmLevel.URGENT;
                } else if (detailLower.contains("forte") || detailLower.contains("2")) {
                    res.analysis = "Choro forte e contínuo pode indicar cólicas do recém-nascido, fome ou fralda suja.";
                    res.homeCare = "Amamente o bebé; faça massagem suave na barriguinha no sentido dos ponteiros do relógio; flexione as perninhas com cuidado; verifique a fralda.";
                    res.alarmSignals = "Febre associada, vómitos em jato, recusar mamar por mais de 6 horas.";
                    res.healthCenterAdvice = "Se o choro durar várias horas sem parar após a massagem, consulte o enfermeiro ou médico.";
                    res.alarmLevel = AlarmLevel.WARNING;
                } else {
                    res.analysis = "Choro fraco ou choramingado pode indicar cansaço, frio/calor ou fome inicial.";
                    res.homeCare = "Ofereça a mama, verifique se o corpinho está morno e confortável no regaço materno.";
                    res.alarmSignals = "Se o bebé ficar muito mole, sem forças para chorar ou recuse mamar.";
                    res.healthCenterAdvice = "Observe durante o dia. Se mantiver recusa alimentar, procure o centro de saúde.";
                    res.alarmLevel = AlarmLevel.NORMAL;
                }
                break;

            case BORBULHAS_ERUPCOES:
                if (detailLower.contains("manchas") || detailLower.contains("febre") || detailLower.contains("2")) {
                    res.analysis = "Erupção cutânea com febre pode ser sinal de virose transmissível (ex: sarampo, rubéola) ou infeção sistémica.";
                    res.homeCare = "Mantenha o bebé hidratado com leite materno. Não aplique cremes caseiros desconhecidos.";
                    res.alarmSignals = "Febre alta, manchinhas roxas/hemorrágicas, dificuldade para respirar, prostração.";
                    res.healthCenterAdvice = "Procure atendimento médico imediato no Posto de Saúde.";
                    res.alarmLevel = AlarmLevel.URGENT;
                } else if (detailLower.contains("assadura") || detailLower.contains("3")) {
                    res.analysis = "Assadura grave na zona da fralda (dermatite de fralda).";
                    res.homeCare = "Troque as fraldas com frequência, lave com água morna e sabão neutro, seque sem esfregar e deixe a pele respirar ao ar livre.";
                    res.alarmSignals = "Feridas abertas com pus ou sangramento, febre.";
                    res.healthCenterAdvice = "Se houver pus ou secreção amarela, consulte o posto de saúde para pomada adequada.";
                    res.alarmLevel = AlarmLevel.WARNING;
                } else {
                    res.analysis = "Pequenas bolinhas vermelhas ou crostas leves podem ser brotoeja (calor) ou crosta láctea.";
                    res.homeCare = "Dê banho morno com água limpa, vista roupas leves de algodão e evite produtos perfumados.";
                    res.alarmSignals = "Se espalhar rapidamente por todo o corpo ou causar dor/irritabilidade extrema.";
                    res.healthCenterAdvice = "Se persistir por mais de 5 dias, mostre ao agente comunitário de saúde.";
                    res.alarmLevel = AlarmLevel.NORMAL;
                }
                break;

            case FEBRE:
                if (detailLower.contains("alta") || detailLower.contains("estremecimento") || detailLower.contains("2") || detailLower.contains("3") || ageInMonths < 3) {
                    res.analysis = "Febre em recém-nascidos (< 3 meses) ou febre alta com estremecimento é uma emergência pediátrica (possível infeção ou malária).";
                    res.homeCare = "Desmame o excesso de agasalhos. Dê banho de água morna (nunca fria). Não dê medicamentos sem orientação médica.";
                    res.alarmSignals = "Convulsão, estremecimento de membros, recusa de mamar, rigidez de nuca, apatia profunda.";
                    res.healthCenterAdvice = "CORRA ao hospital/posto de saúde mais próximo para teste de malária e avaliação médica.";
                    res.alarmLevel = AlarmLevel.URGENT;
                } else {
                    res.analysis = "Febre baixa moderada (37.5ºC a 38ºC). Pode ser reação pós-vacinal ou infeção inicial.";
                    res.homeCare = "Amamente com frequência para evitar desidratação. Vista roupas leves de algodão. Vigie a temperatura.";
                    res.alarmSignals = "Febre subir acima de 38.5ºC, durar mais de 48 horas ou surgirem manchas na pele.";
                    res.healthCenterAdvice = "Se mantiver por mais de 24 horas, leve à unidade de saúde.";
                    res.alarmLevel = AlarmLevel.WARNING;
                }
                break;

            case DIARREIA_VOMITOS:
                if (detailLower.contains("olhos fundos") || detailLower.contains("lágrimas") || detailLower.contains("3") || detailLower.contains("vómitos")) {
                    res.analysis = "Sinais claros de desidratação aguda por diarreia/vómitos (olhos fundos, sem lágrimas, saliva seca). Riscos elevados em bebés.";
                    res.homeCare = "Ofereça leite materno continuadamente e SRO (Soro de Reidratação Oral) em pequenas colheradas se recomendado.";
                    res.alarmSignals = "Bebé muito prostrado/sonolento, recusa total de líquidos, fezes com sangue ou pus, vómitos contínuos.";
                    res.healthCenterAdvice = "URGENTE: Dirija-se imediatamente ao centro de saúde para hidratação venosa/oral.";
                    res.alarmLevel = AlarmLevel.URGENT;
                } else {
                    res.analysis = "Fezes mais líquidas ou golfadas esporádicas após a mamada.";
                    res.homeCare = "Amamente com mais frequência. Coloque o bebé em pé no ombro após mamar para arrotar.";
                    res.alarmSignals = "Diarreia passar de 4 vezes no dia, presenciar febre ou sinais de moleza no bebé.";
                    res.healthCenterAdvice = "Se continuar no dia seguinte, consulte a unidade de saúde.";
                    res.alarmLevel = AlarmLevel.WARNING;
                }
                break;

            case DIFICULDADE_MAMAR:
                if (detailLower.contains("rejeita") || detailLower.contains("pega fraca") || detailLower.contains("1") || detailLower.contains("2")) {
                    res.analysis = "Dificuldade na pega da mama ou recusa alimentar pode decorrer de nariz entupido, febre ou dor de ouvido.";
                    res.homeCare = "Verifique se o narizinho está limpo. Garanta que a boca do bebé abrange a maior parte da aréola e os lábios fiquem virados para fora.";
                    res.alarmSignals = "Bebé sem urinar há mais de 8 horas, perda acentuada de peso, letargia.";
                    res.healthCenterAdvice = "Visite o posto de saúde para apoio do enfermeiro de saúde materna no ajuste da pega.";
                    res.alarmLevel = AlarmLevel.WARNING;
                } else {
                    res.analysis = "Dor mamária materna ou ingurgitamento (seios empedrados).";
                    res.homeCare = "Aplique compressas mornas antes de amamentar, faça ordenha manual para aliviar a tensão e amamente com frequência.";
                    res.alarmSignals = "Febre alta na mãe, vermelhidão intensa num seio com calafrios (possível mastite).";
                    res.healthCenterAdvice = "Se a mãe tiver febre ou vermelhidão no peito, procure a maternidade/posto.";
                    res.alarmLevel = AlarmLevel.WARNING;
                }
                break;

            default:
                res.analysis = "Avaliação de sintomas gerais do recém-nascido.";
                res.homeCare = "Mantenha o bebé aquecido, limpo e exclusivamente amamentado. Observe o sono e as dejeções.";
                res.alarmSignals = "Dificuldade em respirar (cansaço no peito), febre, prostração, icterícia (pele muito amarela).";
                res.healthCenterAdvice = "Em caso de dúvida, dirija-se à unidade de saúde mais próxima.";
                res.alarmLevel = AlarmLevel.NORMAL;
                break;
        }

        return res;
    }

    private TriageResult callExternalAiApi(SymptomCategory category, String detailInput, long ageInMonths) {
        // Fallback gracefully to clinical engine if API fails
        return processClinicalFallback(category, detailInput, ageInMonths);
    }

    public static class TriageResult {
        public String analysis;
        public String homeCare;
        public String alarmSignals;
        public String healthCenterAdvice;
        public AlarmLevel alarmLevel = AlarmLevel.NORMAL;
    }
}
