package ao.co.kutatelamama.dto;

import java.util.List;
import java.util.Map;

public class DashboardSummaryDto {
    private long totalMothers;
    private long totalBabies;
    private long wellVaccinatedBabies;
    private long pendingVaccinatedBabies;
    private double vaccinationRate;
    private Map<String, Integer> ageBreakdown;
    private List<MonthlyStatDto> monthlyStats;
    private Map<String, Integer> provinceBreakdown;

    public DashboardSummaryDto() {}

    public long getTotalMothers() {
        return totalMothers;
    }

    public void setTotalMothers(long totalMothers) {
        this.totalMothers = totalMothers;
    }

    public long getTotalBabies() {
        return totalBabies;
    }

    public void setTotalBabies(long totalBabies) {
        this.totalBabies = totalBabies;
    }

    public long getWellVaccinatedBabies() {
        return wellVaccinatedBabies;
    }

    public void setWellVaccinatedBabies(long wellVaccinatedBabies) {
        this.wellVaccinatedBabies = wellVaccinatedBabies;
    }

    public long getPendingVaccinatedBabies() {
        return pendingVaccinatedBabies;
    }

    public void setPendingVaccinatedBabies(long pendingVaccinatedBabies) {
        this.pendingVaccinatedBabies = pendingVaccinatedBabies;
    }

    public double getVaccinationRate() {
        return vaccinationRate;
    }

    public void setVaccinationRate(double vaccinationRate) {
        this.vaccinationRate = vaccinationRate;
    }

    public Map<String, Integer> getAgeBreakdown() {
        return ageBreakdown;
    }

    public void setAgeBreakdown(Map<String, Integer> ageBreakdown) {
        this.ageBreakdown = ageBreakdown;
    }

    public List<MonthlyStatDto> getMonthlyStats() {
        return monthlyStats;
    }

    public void setMonthlyStats(List<MonthlyStatDto> monthlyStats) {
        this.monthlyStats = monthlyStats;
    }

    public Map<String, Integer> getProvinceBreakdown() {
        return provinceBreakdown;
    }

    public void setProvinceBreakdown(Map<String, Integer> provinceBreakdown) {
        this.provinceBreakdown = provinceBreakdown;
    }

    public static class MonthlyStatDto {
        private int month;
        private String monthLabel;
        private int totalBabies;
        private int wellVaccinatedBabies;
        private int pendingVaccinatedBabies;

        public MonthlyStatDto() {}

        public MonthlyStatDto(int month, String monthLabel, int totalBabies, int wellVaccinatedBabies, int pendingVaccinatedBabies) {
            this.month = month;
            this.monthLabel = monthLabel;
            this.totalBabies = totalBabies;
            this.wellVaccinatedBabies = wellVaccinatedBabies;
            this.pendingVaccinatedBabies = pendingVaccinatedBabies;
        }

        public int getMonth() {
            return month;
        }

        public void setMonth(int month) {
            this.month = month;
        }

        public String getMonthLabel() {
            return monthLabel;
        }

        public void setMonthLabel(String monthLabel) {
            this.monthLabel = monthLabel;
        }

        public int getTotalBabies() {
            return totalBabies;
        }

        public void setTotalBabies(int totalBabies) {
            this.totalBabies = totalBabies;
        }

        public int getWellVaccinatedBabies() {
            return wellVaccinatedBabies;
        }

        public void setWellVaccinatedBabies(int wellVaccinatedBabies) {
            this.wellVaccinatedBabies = wellVaccinatedBabies;
        }

        public int getPendingVaccinatedBabies() {
            return pendingVaccinatedBabies;
        }

        public void setPendingVaccinatedBabies(int pendingVaccinatedBabies) {
            this.pendingVaccinatedBabies = pendingVaccinatedBabies;
        }
    }
}
