package ao.co.kutatelamama.domain.entity;

import ao.co.kutatelamama.domain.enums.VaccineStatus;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "vaccination_records")
public class VaccinationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "baby_id", nullable = false)
    private Baby baby;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "vaccine_id", nullable = false)
    private Vaccine vaccine;

    @Column(nullable = false)
    private LocalDate scheduledDate;

    private LocalDate administeredDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VaccineStatus status = VaccineStatus.SCHEDULED;

    private String healthCenterName;

    public VaccinationRecord() {}

    public VaccinationRecord(Baby baby, Vaccine vaccine, LocalDate scheduledDate, VaccineStatus status, String healthCenterName) {
        this.baby = baby;
        this.vaccine = vaccine;
        this.scheduledDate = scheduledDate;
        this.status = status;
        this.healthCenterName = healthCenterName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Baby getBaby() {
        return baby;
    }

    public void setBaby(Baby baby) {
        this.baby = baby;
    }

    public Vaccine getVaccine() {
        return vaccine;
    }

    public void setVaccine(Vaccine vaccine) {
        this.vaccine = vaccine;
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
