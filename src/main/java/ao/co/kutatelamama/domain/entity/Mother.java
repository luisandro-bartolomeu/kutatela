package ao.co.kutatelamama.domain.entity;

import ao.co.kutatelamama.domain.enums.Language;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "mothers")
public class Mother {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String phoneNumber;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String province;

    @Column(nullable = false)
    private String municipality;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Language preferredLanguage = Language.PORTUGUESE;

    @OneToMany(mappedBy = "mother", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Baby> babies = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Mother() {}

    public Mother(String phoneNumber, String fullName, String province, String municipality, Language preferredLanguage) {
        this.phoneNumber = phoneNumber;
        this.fullName = fullName;
        this.province = province;
        this.municipality = municipality;
        this.preferredLanguage = preferredLanguage != null ? preferredLanguage : Language.PORTUGUESE;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
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

    public Language getPreferredLanguage() {
        return preferredLanguage;
    }

    public void setPreferredLanguage(Language preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
    }

    public List<Baby> getBabies() {
        return babies;
    }

    public void setBabies(List<Baby> babies) {
        this.babies = babies;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
