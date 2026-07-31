package ao.co.kutatelamama.dto;

import java.time.LocalDate;
import java.util.List;

public class BabyDashboardDto {
    private Long id;
    private String fullName;
    private String gender;
    private LocalDate birthDate;
    private long ageInMonths;
    private Long motherId;
    private String motherName;
    private String motherPhone;
    private String province;
    private String municipality;
    private boolean isWellVaccinated;
    private String statusLabel; // "BEM_VACINADO" or "VACINAS_EM_FALTA"
    private int pendingVaccinesCount;
    private int completedVaccinesCount;
    private List<VaccineRecordDto> pendingVaccines;
    private List<VaccineRecordDto> completedVaccines;

    public BabyDashboardDto() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public long getAgeInMonths() {
        return ageInMonths;
    }

    public void setAgeInMonths(long ageInMonths) {
        this.ageInMonths = ageInMonths;
    }

    public Long getMotherId() {
        return motherId;
    }

    public void setMotherId(Long motherId) {
        this.motherId = motherId;
    }

    public String getMotherName() {
        return motherName;
    }

    public void setMotherName(String motherName) {
        this.motherName = motherName;
    }

    public String getMotherPhone() {
        return motherPhone;
    }

    public void setMotherPhone(String motherPhone) {
        this.motherPhone = motherPhone;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getMunicipality() {
        return municipality;
    }

    public void setMunicipality(String municipality) {
        this.municipality = municipality;
    }

    public boolean isWellVaccinated() {
        return isWellVaccinated;
    }

    public void setWellVaccinated(boolean wellVaccinated) {
        isWellVaccinated = wellVaccinated;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public void setStatusLabel(String statusLabel) {
        this.statusLabel = statusLabel;
    }

    public int getPendingVaccinesCount() {
        return pendingVaccinesCount;
    }

    public void setPendingVaccinesCount(int pendingVaccinesCount) {
        this.pendingVaccinesCount = pendingVaccinesCount;
    }

    public int getCompletedVaccinesCount() {
        return completedVaccinesCount;
    }

    public void setCompletedVaccinesCount(int completedVaccinesCount) {
        this.completedVaccinesCount = completedVaccinesCount;
    }

    public List<VaccineRecordDto> getPendingVaccines() {
        return pendingVaccines;
    }

    public void setPendingVaccines(List<VaccineRecordDto> pendingVaccines) {
        this.pendingVaccines = pendingVaccines;
    }

    public List<VaccineRecordDto> getCompletedVaccines() {
        return completedVaccines;
    }

    public void setCompletedVaccines(List<VaccineRecordDto> completedVaccines) {
        this.completedVaccines = completedVaccines;
    }
}
