package ao.co.kutatelamama.domain.entity;

import ao.co.kutatelamama.domain.enums.AlarmLevel;
import ao.co.kutatelamama.domain.enums.SymptomCategory;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "triage_records")
public class TriageRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mother_id")
    private Mother mother;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "baby_id")
    private Baby baby;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SymptomCategory symptomCategory;

    @Column(nullable = false, length = 1000)
    private String symptomDetail;

    @Column(columnDefinition = "TEXT")
    private String aiAnalysis;

    @Column(columnDefinition = "TEXT")
    private String homeCareRecommendations;

    @Column(columnDefinition = "TEXT")
    private String alarmSignals;

    @Column(columnDefinition = "TEXT")
    private String healthCenterAdvice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlarmLevel alarmLevel = AlarmLevel.NORMAL;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public TriageRecord() {}

    public TriageRecord(Mother mother, Baby baby, SymptomCategory symptomCategory, String symptomDetail,
                        String aiAnalysis, String homeCareRecommendations, String alarmSignals,
                        String healthCenterAdvice, AlarmLevel alarmLevel) {
        this.mother = mother;
        this.baby = baby;
        this.symptomCategory = symptomCategory;
        this.symptomDetail = symptomDetail;
        this.aiAnalysis = aiAnalysis;
        this.homeCareRecommendations = homeCareRecommendations;
        this.alarmSignals = alarmSignals;
        this.healthCenterAdvice = healthCenterAdvice;
        this.alarmLevel = alarmLevel;
        this.createdAt = LocalDateTime.now();
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

    public Baby getBaby() {
        return baby;
    }

    public void setBaby(Baby baby) {
        this.baby = baby;
    }

    public SymptomCategory getSymptomCategory() {
        return symptomCategory;
    }

    public void setSymptomCategory(SymptomCategory symptomCategory) {
        this.symptomCategory = symptomCategory;
    }

    public String getSymptomDetail() {
        return symptomDetail;
    }

    public void setSymptomDetail(String symptomDetail) {
        this.symptomDetail = symptomDetail;
    }

    public String getAiAnalysis() {
        return aiAnalysis;
    }

    public void setAiAnalysis(String aiAnalysis) {
        this.aiAnalysis = aiAnalysis;
    }

    public String getHomeCareRecommendations() {
        return homeCareRecommendations;
    }

    public void setHomeCareRecommendations(String homeCareRecommendations) {
        this.homeCareRecommendations = homeCareRecommendations;
    }

    public String getAlarmSignals() {
        return alarmSignals;
    }

    public void setAlarmSignals(String alarmSignals) {
        this.alarmSignals = alarmSignals;
    }

    public String getHealthCenterAdvice() {
        return healthCenterAdvice;
    }

    public void setHealthCenterAdvice(String healthCenterAdvice) {
        this.healthCenterAdvice = healthCenterAdvice;
    }

    public AlarmLevel getAlarmLevel() {
        return alarmLevel;
    }

    public void setAlarmLevel(AlarmLevel alarmLevel) {
        this.alarmLevel = alarmLevel;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
