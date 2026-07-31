package ao.co.kutatelamama.service;

import ao.co.kutatelamama.domain.entity.Baby;
import ao.co.kutatelamama.domain.entity.Mother;
import ao.co.kutatelamama.domain.entity.VaccinationRecord;
import ao.co.kutatelamama.dto.*;
import ao.co.kutatelamama.repository.BabyRepository;
import ao.co.kutatelamama.repository.MotherRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final MotherRepository motherRepository;
    private final BabyRepository babyRepository;
    private final VaccinationService vaccinationService;
    private final SmsService smsService;

    public DashboardService(MotherRepository motherRepository,
                            BabyRepository babyRepository,
                            VaccinationService vaccinationService,
                            SmsService smsService) {
        this.motherRepository = motherRepository;
        this.babyRepository = babyRepository;
        this.vaccinationService = vaccinationService;
        this.smsService = smsService;
    }

    public DashboardSummaryDto getSummaryData() {
        List<Baby> allBabies = babyRepository.findAll();
        long totalMothers = motherRepository.count();
        long totalBabies = allBabies.size();

        long wellVaccinatedCount = 0;
        long pendingVaccinatedCount = 0;

        Map<String, Integer> ageBreakdown = new LinkedHashMap<>();
        ageBreakdown.put("0 Meses", 0);
        ageBreakdown.put("1-2 Meses", 0);
        ageBreakdown.put("3-5 Meses", 0);
        ageBreakdown.put("6-8 Meses", 0);
        ageBreakdown.put("9-11 Meses", 0);
        ageBreakdown.put("12+ Meses", 0);

        Map<String, Integer> provinceBreakdown = new HashMap<>();

        Map<Integer, int[]> monthlyMap = new TreeMap<>(); // month -> [total, well, pending]
        int[] milestoneMonths = {0, 2, 4, 6, 9, 15};
        for (int m : milestoneMonths) {
            monthlyMap.put(m, new int[]{0, 0, 0});
        }

        for (Baby b : allBabies) {
            vaccinationService.ensureVaccinationRecordsExist(b);
            boolean isWell = vaccinationService.isBabyWellVaccinatedForAge(b);
            if (isWell) {
                wellVaccinatedCount++;
            } else {
                pendingVaccinatedCount++;
            }

            long age = b.getAgeInMonths();
            if (age == 0) ageBreakdown.put("0 Meses", ageBreakdown.get("0 Meses") + 1);
            else if (age <= 2) ageBreakdown.put("1-2 Meses", ageBreakdown.get("1-2 Meses") + 1);
            else if (age <= 5) ageBreakdown.put("3-5 Meses", ageBreakdown.get("3-5 Meses") + 1);
            else if (age <= 8) ageBreakdown.put("6-8 Meses", ageBreakdown.get("6-8 Meses") + 1);
            else if (age <= 11) ageBreakdown.put("9-11 Meses", ageBreakdown.get("9-11 Meses") + 1);
            else ageBreakdown.put("12+ Meses", ageBreakdown.get("12+ Meses") + 1);

            Mother m = b.getMother();
            if (m != null && m.getProvince() != null) {
                provinceBreakdown.put(m.getProvince(), provinceBreakdown.getOrDefault(m.getProvince(), 0) + 1);
            }

            // Milestone month breakdown
            int targetMonth = 0;
            if (age >= 15) targetMonth = 15;
            else if (age >= 9) targetMonth = 9;
            else if (age >= 6) targetMonth = 6;
            else if (age >= 4) targetMonth = 4;
            else if (age >= 2) targetMonth = 2;
            else targetMonth = 0;

            if (monthlyMap.containsKey(targetMonth)) {
                int[] stats = monthlyMap.get(targetMonth);
                stats[0]++;
                if (isWell) stats[1]++;
                else stats[2]++;
            }
        }

        DashboardSummaryDto summary = new DashboardSummaryDto();
        summary.setTotalMothers(totalMothers);
        summary.setTotalBabies(totalBabies);
        summary.setWellVaccinatedBabies(wellVaccinatedCount);
        summary.setPendingVaccinatedBabies(pendingVaccinatedCount);
        summary.setVaccinationRate(totalBabies > 0 ? Math.round((double) wellVaccinatedCount / totalBabies * 1000.0) / 10.0 : 0.0);
        summary.setAgeBreakdown(ageBreakdown);
        summary.setProvinceBreakdown(provinceBreakdown);

        List<DashboardSummaryDto.MonthlyStatDto> monthlyList = new ArrayList<>();
        monthlyMap.forEach((month, stats) -> {
            String label = month == 0 ? "Ao Nascer (0M)" : month + " Meses";
            monthlyList.add(new DashboardSummaryDto.MonthlyStatDto(month, label, stats[0], stats[1], stats[2]));
        });
        summary.setMonthlyStats(monthlyList);

        return summary;
    }

    public List<BabyDashboardDto> getBabiesForDashboard(Integer ageMonthsFilter, String statusFilter) {
        List<Baby> allBabies = babyRepository.findAll();
        List<BabyDashboardDto> dtos = new ArrayList<>();

        for (Baby b : allBabies) {
            vaccinationService.ensureVaccinationRecordsExist(b);
            List<VaccinationRecord> pendingRecords = vaccinationService.getPendingVaccinesForBabyAge(b);
            List<VaccinationRecord> completedRecords = vaccinationService.getCompletedVaccinesForBaby(b);

            boolean isWell = pendingRecords.isEmpty();

            // Filter logic
            if (ageMonthsFilter != null && b.getAgeInMonths() != ageMonthsFilter) {
                continue;
            }
            if (statusFilter != null && !statusFilter.isBlank()) {
                if ("WELL_VACCINATED".equalsIgnoreCase(statusFilter) && !isWell) {
                    continue;
                }
                if ("PENDING".equalsIgnoreCase(statusFilter) && isWell) {
                    continue;
                }
            }

            BabyDashboardDto dto = new BabyDashboardDto();
            dto.setId(b.getId());
            dto.setFullName(b.getFullName());
            dto.setGender(b.getGender() != null ? b.getGender() : "M");
            dto.setBirthDate(b.getBirthDate());
            dto.setAgeInMonths(b.getAgeInMonths());

            Mother m = b.getMother();
            if (m != null) {
                dto.setMotherId(m.getId());
                dto.setMotherName(m.getFullName());
                dto.setMotherPhone(m.getPhoneNumber());
                dto.setProvince(m.getProvince());
                dto.setMunicipality(m.getMunicipality());
            }

            dto.setWellVaccinated(isWell);
            dto.setStatusLabel(isWell ? "BEM_VACINADO" : "VACINAS_EM_FALTA");

            List<VaccineRecordDto> pendingDtos = pendingRecords.stream().map(this::toVaccineRecordDto).collect(Collectors.toList());
            List<VaccineRecordDto> completedDtos = completedRecords.stream().map(this::toVaccineRecordDto).collect(Collectors.toList());

            dto.setPendingVaccines(pendingDtos);
            dto.setCompletedVaccines(completedDtos);
            dto.setPendingVaccinesCount(pendingDtos.size());
            dto.setCompletedVaccinesCount(completedDtos.size());

            dtos.add(dto);
        }

        return dtos;
    }

    private VaccineRecordDto toVaccineRecordDto(VaccinationRecord r) {
        return new VaccineRecordDto(
            r.getId(),
            r.getVaccine().getId(),
            r.getVaccine().getName(),
            r.getVaccine().getRecommendedAgeMonths(),
            r.getVaccine().getTargetDiseases(),
            r.getScheduledDate(),
            r.getAdministeredDate(),
            r.getStatus(),
            r.getHealthCenterName()
        );
    }

    public Map<String, Object> sendAlertToMother(SendAlertRequestDto request) {
        if (request.getBabyId() == null) {
            throw new IllegalArgumentException("ID do bebé é obrigatório");
        }

        Baby baby = babyRepository.findById(request.getBabyId())
            .orElseThrow(() -> new IllegalArgumentException("Bebé não encontrado com ID: " + request.getBabyId()));

        Mother mother = baby.getMother();
        String recipientPhone = request.getRecipientPhone() != null && !request.getRecipientPhone().isBlank()
            ? request.getRecipientPhone()
            : (mother != null ? mother.getPhoneNumber() : null);

        if (recipientPhone == null) {
            throw new IllegalArgumentException("Telefone do destinatário não encontrado");
        }

        List<VaccinationRecord> pending = vaccinationService.getPendingVaccinesForBabyAge(baby);
        String pendingVaccinesStr = pending.stream()
            .map(r -> r.getVaccine().getName())
            .collect(Collectors.joining(", "));

        String messageContent = request.getCustomMessage();
        if (messageContent == null || messageContent.isBlank()) {
            messageContent = "Kutatela Mama 🌿: Olá " + (mother != null ? mother.getFullName() : "Mãe") +
                ", lembramos que o(a) bebé " + baby.getFullName() + " (" + baby.getAgeInMonths() + " meses) " +
                "tem vacinas pendentes para o seu mês" +
                (!pendingVaccinesStr.isEmpty() ? ": " + pendingVaccinesStr : "") +
                ". Por favor vá ao centro de saúde mais próximo de " + (mother != null && mother.getProvince() != null ? mother.getProvince() : "sua localidade") + ".";
        }

        String channel = request.getChannel() != null ? request.getChannel().toUpperCase() : "SMS";
        smsService.sendSms(recipientPhone, "ALERT_VACCINE_" + channel, messageContent);

        Map<String, Object> res = new HashMap<>();
        res.put("status", "SUCCESS");
        res.put("babyId", baby.getId());
        res.put("babyName", baby.getFullName());
        res.put("recipientPhone", recipientPhone);
        res.put("channel", channel);
        res.put("messageSent", messageContent);
        res.put("sentAt", java.time.LocalDateTime.now());
        return res;
    }
}
