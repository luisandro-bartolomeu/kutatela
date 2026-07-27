package ao.co.kutatelamama.service;

import ao.co.kutatelamama.domain.entity.Baby;
import ao.co.kutatelamama.domain.entity.Mother;
import ao.co.kutatelamama.domain.entity.TriageRecord;
import ao.co.kutatelamama.domain.entity.WeeklyTip;
import ao.co.kutatelamama.domain.enums.SymptomCategory;
import ao.co.kutatelamama.repository.WeeklyTipRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Optional;

@Service
public class UssdSessionService {

    private static final Logger log = LoggerFactory.getLogger(UssdSessionService.class);

    private static final String[] ANGOLA_PROVINCES = {
        "Bengo", "Benguela", "Bié", "Cabinda", "Cuando Cubango",
        "Cuanza Norte", "Cuanza Sul", "Cunene", "Huambo", "Huíla",
        "Luanda", "Lunda Norte", "Lunda Sul", "Malanje", "Moxico",
        "Namibe", "Uíge", "Zaire"
    };

    private final MotherService motherService;
    private final VaccinationService vaccinationService;
    private final TriageAiService triageAiService;
    private final WeeklyTipRepository weeklyTipRepository;
    private final SmsService smsService;

    public UssdSessionService(MotherService motherService,
                              VaccinationService vaccinationService,
                              TriageAiService triageAiService,
                              WeeklyTipRepository weeklyTipRepository,
                              SmsService smsService) {
        this.motherService = motherService;
        this.vaccinationService = vaccinationService;
        this.triageAiService = triageAiService;
        this.weeklyTipRepository = weeklyTipRepository;
        this.smsService = smsService;
    }

    /**
     * Remove acentos e emojis para mensagens USSD (aparelhos analógicos simples)
     */
    public static String stripAccentsAndEmojis(String input) {
        if (input == null || input.isEmpty()) return input;

        // 1. Remove Emojis, símbolos Unicode e pares substitutos (surrogates)
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); ) {
            int codePoint = input.codePointAt(i);
            int charCount = Character.charCount(codePoint);
            i += charCount;

            if (isEmojiOrSymbol(codePoint)) {
                continue;
            }
            sb.appendCodePoint(codePoint);
        }
        String noEmojis = sb.toString();

        // 2. Normalização NFD e remoção de marcas diacríticas (acentos)
        String normalized = Normalizer.normalize(noEmojis, Normalizer.Form.NFD);
        String noAccents = normalized.replaceAll("\\p{M}", "");

        // 3. Substituições de caracteres especiais mantidos pela NFD
        noAccents = noAccents.replace("ç", "c")
                             .replace("Ç", "C")
                             .replace("º", "")
                             .replace("ª", "")
                             .replace("–", "-")
                             .replace("—", "-")
                             .replace("“", "\"")
                             .replace("”", "\"")
                             .replace("‘", "'")
                             .replace("’", "'");

        // 4. Limpeza de espaços duplicados mantendo a estrutura de quebras de linha
        String[] lines = noAccents.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            lines[i] = lines[i].replaceAll("[ \t]+", " ").trim();
        }
        return String.join("\n", lines);
    }

    private static boolean isEmojiOrSymbol(int codePoint) {
        if (codePoint >= 0x1F300 && codePoint <= 0x1F9FF) return true; // Misc Symbols & Pictographs
        if (codePoint >= 0x1F600 && codePoint <= 0x1F64F) return true; // Emoticons
        if (codePoint >= 0x1F680 && codePoint <= 0x1F6FF) return true; // Transport & Map
        if (codePoint >= 0x2600  && codePoint <= 0x27BF)  return true; // Misc Symbols & Dingbats
        if (codePoint >= 0x1F1E6 && codePoint <= 0x1F1FF) return true; // Regional Indicator Flags
        if (codePoint >= 0x1F900 && codePoint <= 0x1F9FF) return true; // Supplemental Symbols
        if (codePoint >= 0x1FA70 && codePoint <= 0x1FAFF) return true; // Symbols Extended-A
        if (codePoint == 0x200D || codePoint == 0xFE0F) return true;   // ZWJ / Variation Selector

        int type = Character.getType(codePoint);
        return type == Character.OTHER_SYMBOL || type == Character.SURROGATE;
    }

    public String processUssdRequest(String sessionId, String serviceCode, String phoneNumber, String text) {
        log.info("[USSD Request] sessionId: {}, serviceCode: {}, phone: {}, text: '{}'", sessionId, serviceCode, phoneNumber, text);

        String[] parts = cleanAndReduceUssdPath(text);

        // Fetch or create default mother with normalized phone number
        String cleanPhone = motherService.normalizePhoneNumber(phoneNumber);
        Mother mother = motherService.findByPhoneNumber(cleanPhone).orElseGet(() -> {
            return motherService.registerMotherAndBaby(cleanPhone, "Mãe " + cleanPhone.substring(Math.max(0, cleanPhone.length() - 4)), "Huambo", "Bebé", 2);
        });

        Baby baby = motherService.getOrCreateDefaultBabyForMother(mother);

        // Root menu level
        if (parts.length == 0) {
            return buildMainMenu(mother);
        }

        String mainChoice = parts[0];

        switch (mainChoice) {
            case "1":
                return handleVaccinationMenu(parts, mother, baby);
            case "2":
                return handleTriageMenu(parts, mother, baby);
            case "3":
                return handleWeeklyTipsMenu(parts, mother, baby);
            case "4":
                return handleRegistrationMenu(parts, mother, phoneNumber);
            case "5":
                return "END 🟢 Obrigado por usar o Kutatela Mama.\nCuidar da mãe é cuidar do futuro!";
            default:
                return "CON Opção inválida.\n" + buildMainMenu(mother);
        }
    }

    /**
     * Processa a sequência de entradas USSD acumuladas pelo Africa's Talking / WhatsApp (ex: "1*1*0*2"),
     * decodificando caracteres como %2A (*), ignorando códigos de serviço e interpretando 
     * '0' como recuar 1 nível e '00' como voltar ao menu principal.
     */
    public String[] cleanAndReduceUssdPath(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new String[0];
        }

        String raw = text.trim();
        try {
            raw = java.net.URLDecoder.decode(raw, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception ignored) {}

        raw = raw.replace("%2A", "*").replace("%2a", "*").replace("#", "").trim();

        String[] tokens = raw.split("\\*");
        java.util.List<String> stack = new java.util.ArrayList<>();

        for (String token : tokens) {
            String t = token.trim();
            if (t.isEmpty()) continue;

            // Ignora o código de serviço caso venha incluído no texto (ex: 384, 23898, 123)
            if (stack.isEmpty() && (t.contains("384") || t.contains("23898") || t.contains("123"))) {
                continue;
            }

            // Se ja estiver no resultado da Triagem (pilha tamanho >= 3 na opcao 2)
            if (stack.size() >= 3 && "2".equals(stack.get(0))) {
                if ("1".equals(t)) {
                    stack.clear();
                    stack.add("2"); // Volta ao menu de Triagem
                    continue;
                } else if ("0".equals(t) || "00".equals(t)) {
                    stack.clear(); // Volta ao menu principal
                    continue;
                }
            }

            // Se ja estiver na tela de confirmacao de Registar/Atualizar Dados (pilha tamanho >= 3 na opcao 4)
            if (stack.size() >= 3 && "4".equals(stack.get(0))) {
                if ("0".equals(t) || "00".equals(t)) {
                    stack.clear(); // Volta ao menu principal
                    continue;
                } else {
                    stack.clear();
                    stack.add("4"); // Volta ao menu de registo
                    continue;
                }
            }

            // Em Vacinação (1) e Dicas (3), o nível 2 ("1*1", "3*1") é uma tela final/folha com '0. Voltar'.
            boolean isLeafScreenState = stack.size() >= 2 && ("1".equals(stack.get(0)) || "3".equals(stack.get(0)));

            // Verifica se está na fase de introdução de dados livres (opção 4 ou opção 2->6 texto livre)
            boolean isDataInputState = (stack.size() == 2 && ("4".equals(stack.get(0)) || ("2".equals(stack.get(0)) && "6".equals(stack.get(1)))));

            if ("00".equals(t)) {
                stack.clear();
            } else if ("0".equals(t) && !isDataInputState) {
                if (!stack.isEmpty()) {
                    stack.remove(stack.size() - 1);
                }
            } else {
                if (!isLeafScreenState) {
                    stack.add(t);
                }
            }
        }
        return stack.toArray(new String[0]);
    }

    private String buildMainMenu(Mother mother) {
        return "CON Olá! Bem-vinda ao Kutatela Mama\n" +
               "Mãe: " + mother.getFullName() + "\n" +
               "================================\n" +
               "Escolha uma opção:\n" +
               "1. Vacinação\n" +
               "2. Triagem de Sintomas (IA)\n" +
               "3. Dicas de Cuidados\n" +
               "4. Registar / Atualizar Dados\n" +
               "5. Sair";
    }

    private String handleVaccinationMenu(String[] parts, Mother mother, Baby baby) {
        if (parts.length == 1) {
            return "CON Calendário de Vacinação\n" +
                   "================================\n" +
                   "1. Ver próximas vacinas do bebé\n" +
                   "2. Calendário completo nacional\n" +
                   "3. Unidade de saúde mais próxima\n" +
                   "0. Voltar ao menu principal";
        }

        String choice = parts[1];
        if ("0".equals(choice)) {
            return buildMainMenu(mother);
        }

        switch (choice) {
            case "1":
                String upcoming = vaccinationService.formatUpcomingVaccinesForBaby(baby);
                return "CON " + upcoming + "\n0. Voltar ao menu principal";
            case "2":
                String calendar = vaccinationService.formatFullNationalCalendar();
                return "CON " + calendar + "\n0. Voltar ao menu principal";
            case "3":
                String healthCenter = vaccinationService.getNearestHealthCenter(mother.getProvince());
                return "CON " + healthCenter + "\n0. Voltar ao menu principal";
            default:
                return "CON Opção inválida.\n0. Voltar ao menu principal";
        }
    }

    private String handleTriageMenu(String[] parts, Mother mother, Baby baby) {
        if (parts.length == 1) {
            return "CON Triagem de Sintomas com IA\n" +
                   "================================\n" +
                   "O que o seu bebé está a sentir?\n" +
                   "1. Choro persistente\n" +
                   "2. Borbulhas / erupções na pele\n" +
                   "3. Febre\n" +
                   "4. Diarreia / Vómitos\n" +
                   "5. Dificuldade para mamar\n" +
                   "6. Descrever sintoma (Texto livre)\n" +
                   "0. Voltar ao menu principal";
        }

        String symptomChoice = parts[1];
        if ("0".equals(symptomChoice)) {
            return buildMainMenu(mother);
        }

        SymptomCategory category;
        switch (symptomChoice) {
            case "1": category = SymptomCategory.CHORO_PERSISTENTE; break;
            case "2": category = SymptomCategory.BORBULHAS_ERUPCOES; break;
            case "3": category = SymptomCategory.FEBRE; break;
            case "4": category = SymptomCategory.DIARREIA_VOMITOS; break;
            case "5": category = SymptomCategory.DIFICULDADE_MAMAR; break;
            case "6": category = SymptomCategory.OUTRO; break;
            default: return "CON Opção inválida.\n0. Voltar ao menu principal";
        }

        if (parts.length == 2) {
            if ("6".equals(symptomChoice)) {
                return "CON Digite com as suas palavras os sintomas do bebé (ex: febre alta de 39C, diarreia e a chorar muito):";
            }
            return buildSymptomDetailPrompt(category);
        }

        String detailInput = parts[2];
        String detailLabel = "6".equals(symptomChoice) ? detailInput : getDetailLabel(category, detailInput);

        // Perform AI Triage via DeepSeek / Fallback
        TriageRecord record = triageAiService.performTriage(mother, baby, category, detailLabel);

        return formatTriageResponse(record);
    }

    private String buildSymptomDetailPrompt(SymptomCategory category) {
        switch (category) {
            case CHORO_PERSISTENTE:
                return "CON Descreva o choro:\n" +
                       "1. Choro fraco e choramingado\n" +
                       "2. Choro forte e contínuo\n" +
                       "3. Choro agudo e estridente\n" +
                       "4. Choro com gemidos";
            case BORBULHAS_ERUPCOES:
                return "CON Descreva as borbulhas:\n" +
                       "1. Bolinhas vermelhas simples no corpo\n" +
                       "2. Manchas vermelhas com febre\n" +
                       "3. Assadura grave na fralda\n" +
                       "4. Crostas amareladas";
            case FEBRE:
                return "CON Descreva a febre:\n" +
                       "1. Febre baixa (37.5C - 38C)\n" +
                       "2. Febre alta (>38.5C) corpo quente\n" +
                       "3. Febre com estremecimento/prostração";
            case DIARREIA_VOMITOS:
                return "CON Descreva os sintomas:\n" +
                       "1. Fezes muito líquidas (>3x/dia)\n" +
                       "2. Vómitos após cada mamada\n" +
                       "3. Olhos fundos / sem lágrimas";
            case DIFICULDADE_MAMAR:
                return "CON Descreva a dificuldade:\n" +
                       "1. Bebé rejeita a mama / chora ao mamar\n" +
                       "2. Pega fraca / mamada muito curta\n" +
                       "3. Mãe com dor nos seios / empedrados";
            default:
                return "CON Descreva o sintoma:\n" +
                       "1. Tosse ou cansaço no peito\n" +
                       "2. Pele ou olhos amarelados\n" +
                       "3. Olhos remelados / com secreção";
        }
    }

    private String getDetailLabel(SymptomCategory category, String choice) {
        switch (category) {
            case CHORO_PERSISTENTE:
                if ("1".equals(choice)) return "Choro fraco e choramingado";
                if ("2".equals(choice)) return "Choro forte e contínuo";
                if ("3".equals(choice)) return "Choro agudo e estridente";
                return "Choro com gemidos";
            case BORBULHAS_ERUPCOES:
                if ("1".equals(choice)) return "Bolinhas vermelhas simples no corpo";
                if ("2".equals(choice)) return "Manchas vermelhas com febre";
                if ("3".equals(choice)) return "Assadura grave na fralda";
                return "Crostas amareladas";
            case FEBRE:
                if ("1".equals(choice)) return "Febre baixa (37.5C - 38C)";
                if ("2".equals(choice)) return "Febre alta (>38.5C) com corpo quente";
                return "Febre com estremecimento / prostração";
            case DIARREIA_VOMITOS:
                if ("1".equals(choice)) return "Fezes muito líquidas (>3x/dia)";
                if ("2".equals(choice)) return "Vómitos após cada mamada";
                return "Olhos fundos / sem lágrimas (desidratação)";
            case DIFICULDADE_MAMAR:
                if ("1".equals(choice)) return "Bebé rejeita a mama";
                if ("2".equals(choice)) return "Pega fraca / mamada curta";
                return "Mãe com dor intensa / seios empedrados";
            default:
                if ("1".equals(choice)) return "Tosse ou cansaço no peito";
                if ("2".equals(choice)) return "Pele ou olhos amarelados";
                return "Olhos remelados / com secreção";
        }
    }

    private String formatTriageResponse(TriageRecord record) {
        String analysis = record.getAiAnalysis();
        String homeCare = record.getHomeCareRecommendations();
        String healthAdvice = record.getHealthCenterAdvice();

        return "CON Resultado da Triagem:\n" +
               "Análise: " + (analysis != null ? analysis : "Avaliação concluída.") + "\n\n" +
               "Cuidados: " + (homeCare != null ? homeCare : "Consulte o posto de saúde.") + "\n" +
               "Posto de Saúde: " + (healthAdvice != null ? healthAdvice : "Procure a unidade de saúde mais próxima.") + "\n\n" +
               "================================\n" +
               "1. Fazer outra triagem\n" +
               "0. Voltar ao menu principal";
    }

    private String handleWeeklyTipsMenu(String[] parts, Mother mother, Baby baby) {
        long ageMonths = baby != null ? baby.getAgeInMonths() : 1;

        if (parts.length == 1) {
            return "CON Dicas de Cuidados (Bebé: " + ageMonths + " mês(es))\n" +
                   "====================================\n" +
                   "Escolha um tópico:\n" +
                   "1. Amamentação Exclusiva\n" +
                   "2. Higiene do bebé & Coto Umbilical\n" +
                   "3. Sono seguro\n" +
                   "4. Estimulação precoce\n" +
                   "5. Saúde mental materna\n" +
                   "6. Nutrição e Introdução alimentar\n" +
                   "0. Voltar ao menu principal";
        }

        String choice = parts[1];
        if ("0".equals(choice)) {
            return buildMainMenu(mother);
        }

        String cat;
        switch (choice) {
            case "1": cat = "AMAMENTACAO"; break;
            case "2": cat = "HIGIENE"; break;
            case "3": cat = "SONO_SEGURO"; break;
            case "4": cat = "ESTIMULACAO"; break;
            case "5": cat = "SAUDE_MENTAL"; break;
            case "6": cat = "NUTRICAO"; break;
            default: return "CON Opção inválida.\n0. Voltar ao menu principal";
        }

        Optional<WeeklyTip> tipOpt = weeklyTipRepository.findByCategory(cat).stream().findFirst();
        if (tipOpt.isPresent()) {
            WeeklyTip tip = tipOpt.get();
            String tipContent = "Dica para bebé de " + ageMonths + " mês(es) (" + tip.getTitle() + "):\n" + tip.getContentPt();
            smsService.sendSms(mother.getPhoneNumber(), "WEEKLY_CARE_TIP", tipContent);
            return "CON " + tipContent + "\n\n" +
                   "0. Voltar ao menu principal";
        } else {
            return "CON Dica em atualização. Consulte o posto de saúde local.\n0. Voltar ao menu principal";
        }
    }

    private String buildProvincePrompt() {
        StringBuilder sb = new StringBuilder("CON Escolha a Província:\n");
        for (int i = 0; i < ANGOLA_PROVINCES.length; i++) {
            sb.append(i + 1).append(". ").append(ANGOLA_PROVINCES[i]);
            if ((i + 1) % 3 == 0 || i == ANGOLA_PROVINCES.length - 1) {
                sb.append("\n");
            } else {
                sb.append(" | ");
            }
        }
        return sb.toString().trim();
    }

    private String handleRegistrationMenu(String[] parts, Mother mother, String phone) {
        Baby baby = motherService.getOrCreateDefaultBabyForMother(mother);

        if (parts.length == 1) {
            return "CON Registar / Atualizar Dados\n" +
                   "================================\n" +
                   "Nome atual: " + mother.getFullName() + "\n" +
                   "Província: " + mother.getProvince() + "\n" +
                   "Idade Bebé: " + baby.getAgeInMonths() + " mês(es)\n\n" +
                   "1. Atualizar Nome da Mãe\n" +
                   "2. Atualizar Província\n" +
                   "3. Registar Idade do Bebé (meses)\n" +
                   "0. Voltar ao menu principal";
        }

        String choice = parts[1];
        if ("0".equals(choice)) {
            return buildMainMenu(mother);
        }

        if (parts.length == 2) {
            if ("1".equals(choice)) return "CON Digite o seu Nome Completo (ex: Maria Silva):";
            if ("2".equals(choice)) return buildProvincePrompt();
            if ("3".equals(choice)) return "CON Quantos meses tem o seu bebé? (ex: 0, 2, 4, 6):";
        }

        if (parts.length >= 3) {
            String val = parts[2];
            if ("1".equals(choice)) {
                motherService.updateMotherName(mother, val);
                return "CON Nome atualizado com sucesso para " + val + "!\n\n0. Voltar ao menu principal";
            } else if ("2".equals(choice)) {
                String prov = val;
                try {
                    int idx = Integer.parseInt(val.trim());
                    if (idx >= 1 && idx <= ANGOLA_PROVINCES.length) {
                        prov = ANGOLA_PROVINCES[idx - 1];
                    }
                } catch (NumberFormatException ignored) {}
                motherService.updateProvince(mother, prov);
                return "CON Província atualizada para " + prov + "!\n\n0. Voltar ao menu principal";
            } else if ("3".equals(choice)) {
                try {
                    int age = Integer.parseInt(val.trim());
                    motherService.updateBabyAge(mother, age);
                    return "CON Idade do bebé atualizada para " + age + " meses!\n\n0. Voltar ao menu principal";
                } catch (Exception e) {
                    return "CON Formato de idade inválido.\n\n0. Voltar ao menu principal";
                }
            }
        }

        return "CON Opção inválida.\n0. Voltar ao menu principal";
    }
}

