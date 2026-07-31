package ao.co.kutatelamama.dto;

import ao.co.kutatelamama.domain.enums.VaccineStatus;
import java.time.LocalDate;

public class VaccineRecordDto {
    private Long id;
    private Long vaccineId;
    private String vaccineName;
    private int recommendedAgeMonths;
    private String targetDisease;
    private LocalDate scheduledDate;
    private LocalDate administeredDate;
    private VaccineStatus status;
    private String healthCenterName;

    public VaccineRecordDto() {}

    public VaccineRecordDto(Long id, Long vaccineId, String vaccineName, int recommendedAgeMonths, String targetDisease, LocalDate scheduledDate, LocalDate administeredDate, VaccineStatus status, String healthCenterName) {
        this.id = id;
        this.vaccineId = vaccineId;
        this.vaccineName = vaccineName;
        this.recommendedAgeMonths = recommendedAgeMonths;
        this.targetDisease = targetDisease;
        this.scheduledDate = scheduledDate;
        this.administeredDate = administeredDate;
        this.status = status;
        this.healthCenterName = healthCenterName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVaccineId() {
        return vaccineId;
    }

    public void setVaccineId(Long vaccineId) {
        this.vaccineId = vaccineId;
    }

    public String getVaccineName() {
        return vaccineName;
    }

    public void setVaccineName(String vaccineName) {
        this.vaccineName = vaccineName;
    }

    public int getRecommendedAgeMonths() {
        return recommendedAgeMonths;
    }

    public void setRecommendedAgeMonths(int recommendedAgeMonths) {
        this.recommendedAgeMonths = recommendedAgeMonths;
    }

    public String getTargetDisease() {
        return targetDisease;
    }

    public void setTargetDisease(String targetDisease) {
        this.targetDisease = targetDisease;
    }

    public LocalDate getScheduledDate() {
        return scheduledDate;
    }

    public void setScheduledDate(LocalDate scheduledDate) {
        this.scheduledDate = scheduledDate;
    }

    public LocalDate getAdministeredDate() {
        return administeredDate;
    }

    public void setAdministeredDate(LocalDate administeredDate) {
        this.administeredDate = administeredDate;
    }

    public VaccineStatus getStatus() {
        return status;
    }

    public void setStatus(VaccineStatus status) {
        this.status = status;
    }

    public String getHealthCenterName() {
        return healthCenterName;
    }

    public void setHealthCenterName(String healthCenterName) {
        this.healthCenterName = healthCenterName;
    }
}
