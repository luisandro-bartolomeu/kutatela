package ao.co.kutatelamama.domain.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "babies")
public class Baby {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mother_id", nullable = false)
    private Mother mother;

    @Column(nullable = false)
    private String fullName;

    @Column(length = 10)
    private String gender;

    @Column(nullable = false)
    private LocalDate birthDate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Baby() {}

    public Baby(Mother mother, String fullName, String gender, LocalDate birthDate) {
        this.mother = mother;
        this.fullName = fullName;
        this.gender = gender;
        this.birthDate = birthDate;
        this.createdAt = LocalDateTime.now();
    }

    public long getAgeInMonths() {
        if (birthDate == null) return 0;
        return ChronoUnit.MONTHS.between(birthDate, LocalDate.now());
    }

    public long getAgeInWeeks() {
        if (birthDate == null) return 0;
        return ChronoUnit.WEEKS.between(birthDate, LocalDate.now());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Mother getMother() {
        return mother;
    }

    public void setMother(Mother mother) {
        this.mother = mother;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
