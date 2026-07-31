package ao.co.kutatelamama.dto;

import java.time.LocalDate;

public class MarkVaccineRequestDto {
    private Long recordId;
    private Long babyId;
    private Long vaccineId;
    private LocalDate administeredDate;
    private String healthCenterName;

    public MarkVaccineRequestDto() {}

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public Long getBabyId() {
        return babyId;
    }

    public void setBabyId(Long babyId) {
        this.babyId = babyId;
    }

    public Long getVaccineId() {
        return vaccineId;
    }

    public void setVaccineId(Long vaccineId) {
        this.vaccineId = vaccineId;
    }

    public LocalDate getAdministeredDate() {
        return administeredDate;
    }

    public void setAdministeredDate(LocalDate administeredDate) {
        this.administeredDate = administeredDate;
    }

    public String getHealthCenterName() {
        return healthCenterName;
    }

    public void setHealthCenterName(String healthCenterName) {
        this.healthCenterName = healthCenterName;
    }
}
