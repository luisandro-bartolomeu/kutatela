package ao.co.kutatelamama.domain.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "vaccines")
public class Vaccine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private Integer recommendedAgeMonths;

    @Column(nullable = false, length = 500)
    private String targetDiseases;

    @Column(length = 1000)
    private String instructions;

    public Vaccine() {}

    public Vaccine(String name, Integer recommendedAgeMonths, String targetDiseases, String instructions) {
        this.name = name;
        this.recommendedAgeMonths = recommendedAgeMonths;
        this.targetDiseases = targetDiseases;
        this.instructions = instructions;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getRecommendedAgeMonths() {
        return recommendedAgeMonths;
    }

    public void setRecommendedAgeMonths(Integer recommendedAgeMonths) {
        this.recommendedAgeMonths = recommendedAgeMonths;
    }

    public String getTargetDiseases() {
        return targetDiseases;
    }

    public void setTargetDiseases(String targetDiseases) {
        this.targetDiseases = targetDiseases;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }
}
