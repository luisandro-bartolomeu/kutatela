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

import java.util.Optional;

@Service
public class UssdSessionService {

    private static final Logger log = LoggerFactory.getLogger(UssdSessionService.class);

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

    public String processUssdRequest(String sessionId, String serviceCode, String phoneNumber, String text) {
        log.info("📞 USSD Request -> sessionId: {}, serviceCode: {}, phone: {}, text: '{}'", sessionId, serviceCode, phoneNumber, text);

        String trimmedText = text != null ? text.trim() : "";
        String[] parts = trimmedText.isEmpty() ? new String[0] : trimmedText.split("\\*");

        // Fetch or create default mother
        Mother mother = motherService.findByPhoneNumber(phoneNumber).orElseGet(() -> {
            return motherService.registerMotherAndBaby(phoneNumber, "Mãe " + phoneNumber.substring(Math.max(0, phoneNumber.length() - 4)), "Huambo", "Bebé", 2);
        });

        Baby baby = motherService.getOrCreateDefaultBabyForMother(mother);

        // Root menu level
        if (parts.length == 0 || trimmedText.isEmpty()) {
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
                return "END Obrigado por usar o Kutatela Mama 🌿.\nCuidar da mãe é cuidar do futuro!";
            default:
                return "CON Opção inválida.\n" + buildMainMenu(mother);
        }
    }

    private String buildMainMenu(Mother mother) {
        return "CON Olá! Bem-vinda ao Kutatela Mama 🌿\n" +
               "Mãe: " + mother.getFullName() + "\n" +
               "================================\n" +
               "Escolha uma opção:\n" +
               "1. Calendário de Vacinação\n" +
               "2. Triagem de Sintomas (IA)\n" +
               "3. Dicas de Cuidados Semanais\n" +
               "4. Registar / Atualizar Dados\n" +
               "5. Sair";
    }

    private String handleVaccinationMenu(String[] parts, Mother mother, Baby baby) {
        if (parts.length == 1) {
            return "CON Calendário de Vacinação 🌿\n" +
                   "================================\n" +
                   "1. Ver próximas vacinas do bebé\n" +
                   "2. Ver calendário completo nacional\n" +
                   "3. Unidade de saúde mais próxima\n" +
                   "0. Voltar ao menu";
        }

        String choice = parts[1];
        if ("0".equals(choice)) {
            return buildMainMenu(mother);
        }

        switch (choice) {
            case "1":
                String upcoming = vaccinationService.formatUpcomingVaccinesForBaby(baby);
                return "CON " + upcoming + "\nDigite 0 para voltar";
            case "2":
                String calendar = vaccinationService.formatFullNationalCalendar();
                return "CON " + calendar + "\nDigite 0 para voltar";
            case "3":
                String healthCenter = vaccinationService.getNearestHealthCenter(mother.getProvince());
                return "CON " + healthCenter + "\nDigite 0 para voltar";
            default:
                return "CON Opção inválida.\nDigite 0 para voltar";
        }
    }

    private String handleTriageMenu(String[] parts, Mother mother, Baby baby) {
        if (parts.length == 1) {
            return "CON Triagem de Sintomas com IA 🩺\n" +
                   "================================\n" +
                   "O que o seu bebé está a sentir?\n" +
                   "1. Choro persistente\n" +
                   "2. Borbulhas / erupções cutâneas\n" +
                   "3. Febre\n" +
                   "4. Diarreia / Vómitos\n" +
                   "5. Dificuldade para mamar\n" +
                   "6. Outro sintoma\n" +
                   "0. Voltar ao menu";
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
            default: return "CON Opção inválida.\n0. Voltar";
        }

        if (parts.length == 2) {
            return buildSymptomDetailPrompt(category);
        }

        String detailChoice = parts[2];
        String detailLabel = getDetailLabel(category, detailChoice);

        // Perform AI Triage
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
                       "1. Febre baixa (37.5ºC - 38ºC)\n" +
                       "2. Febre alta (>38.5ºC) corpo quente\n" +
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
                if ("1".equals(choice)) return "Febre baixa (37.5ºC - 38ºC)";
                if ("2".equals(choice)) return "Febre alta (>38.5ºC) com corpo quente";
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
        if (analysis != null && analysis.length() > 90) {
            analysis = analysis.substring(0, 87) + "...";
        }
        return "END 🔍 Análise: " + analysis + "\n\n" +
               "📋 Cuidados: " + record.getHomeCareRecommendations() + "\n" +
               "🏥 " + record.getHealthCenterAdvice() + "\n" +
               "📱 Resumo detalhado enviado por SMS!";
    }

    private String handleWeeklyTipsMenu(String[] parts, Mother mother, Baby baby) {
        if (parts.length == 1) {
            return "CON Dicas de Cuidados Semanais 🍼\n" +
                   "====================================\n" +
                   "Escolha um tópico:\n" +
                   "1. Amamentação Exclusiva\n" +
                   "2. Higiene do bebé & Coto Umbilical\n" +
                   "3. Sono seguro\n" +
                   "4. Estimulação precoce\n" +
                   "5. Saúde mental materna\n" +
                   "6. Nutrição e Introdução alimentar\n" +
                   "0. Voltar ao menu";
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
            default: return "CON Opção inválida.\n0. Voltar";
        }

        Optional<WeeklyTip> tipOpt = weeklyTipRepository.findByCategory(cat).stream().findFirst();
        if (tipOpt.isPresent()) {
            WeeklyTip tip = tipOpt.get();
            smsService.sendSms(mother.getPhoneNumber(), "WEEKLY_CARE_TIP", tip.getContentPt());
            return "CON 🍼 " + tip.getTitle() + ":\n" +
                   tip.getContentPt() + "\n\n" +
                   "📱 Enviámos esta dica para o seu SMS!\n" +
                   "Digite 0 para voltar";
        } else {
            return "CON 🍼 Dica em atualização. Consulte o posto de saúde local.\nDigite 0 para voltar";
        }
    }

    private String handleRegistrationMenu(String[] parts, Mother mother, String phone) {
        if (parts.length == 1) {
            return "CON Registar / Atualizar Dados 📝\n" +
                   "================================\n" +
                   "Nome atual: " + mother.getFullName() + "\n" +
                   "Província: " + mother.getProvince() + "\n\n" +
                   "Para atualizar os dados do bebé, selecione:\n" +
                   "1. Atualizar Nome da Mãe\n" +
                   "2. Atualizar Província (Huambo, Benguela, Bié, etc.)\n" +
                   "3. Registar Idade do Bebé (meses)\n" +
                   "0. Voltar ao menu";
        }

        String choice = parts[1];
        if ("0".equals(choice)) {
            return buildMainMenu(mother);
        }

        if (parts.length == 2) {
            if ("1".equals(choice)) return "CON Digite o seu Nome Completo (ex: Maria Silva):";
            if ("2".equals(choice)) return "CON Escolha a Província:\n1. Huambo\n2. Benguela\n3. Bié\n4. Huíla\n5. Luanda";
            if ("3".equals(choice)) return "CON Quantos meses tem o seu bebé? (ex: 0, 2, 4, 6):";
        }

        if (parts.length >= 3) {
            String val = parts[2];
            if ("1".equals(choice)) {
                mother.setFullName(val);
                motherService.registerMotherAndBaby(phone, val, mother.getProvince(), "Bebé de " + val, 2);
                return "END ✅ Nome atualizado com sucesso para " + val + "!";
            } else if ("2".equals(choice)) {
                String prov = "Huambo";
                if ("2".equals(val)) prov = "Benguela";
                if ("3".equals(val)) prov = "Bié";
                if ("4".equals(val)) prov = "Huíla";
                if ("5".equals(val)) prov = "Luanda";
                mother.setProvince(prov);
                motherService.registerMotherAndBaby(phone, mother.getFullName(), prov, "Bebé de " + mother.getFullName(), 2);
                return "END ✅ Província atualizada para " + prov + "!";
            } else if ("3".equals(choice)) {
                try {
                    int age = Integer.parseInt(val);
                    motherService.registerMotherAndBaby(phone, mother.getFullName(), mother.getProvince(), "Bebé de " + mother.getFullName(), age);
                    return "END ✅ Idade do bebé atualizada para " + age + " meses!";
                } catch (Exception e) {
                    return "END Formato de idade inválido.";
                }
            }
        }

        return "CON Opção inválida.\nDigite 0 para voltar";
    }
}
