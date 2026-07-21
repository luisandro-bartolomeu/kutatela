package ao.co.kutatelamama.domain.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "weekly_tips")
public class WeeklyTip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer weekNumber;

    @Column(nullable = false)
    private String category; // AMAMENTACAO, HIGIENE, SONO_SEGURO, ESTIMULACAO, SAUDE_MENTAL, NUTRICAO

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 1000)
    private String contentPt;

    @Column(length = 1000)
    private String contentUmbundu;

    public WeeklyTip() {}

    public WeeklyTip(Integer weekNumber, String category, String title, String contentPt, String contentUmbundu) {
        this.weekNumber = weekNumber;
        this.category = category;
        this.title = title;
        this.contentPt = contentPt;
        this.contentUmbundu = contentUmbundu;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getWeekNumber() {
        return weekNumber;
    }

    public void setWeekNumber(Integer weekNumber) {
        this.weekNumber = weekNumber;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContentPt() {
        return contentPt;
    }

    public void setContentPt(String contentPt) {
        this.contentPt = contentPt;
    }

    public String getContentUmbundu() {
        return contentUmbundu;
    }

    public void setContentUmbundu(String contentUmbundu) {
        this.contentUmbundu = contentUmbundu;
    }
}
