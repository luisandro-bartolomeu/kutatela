package ao.co.kutatelamama.repository;

import ao.co.kutatelamama.domain.entity.Baby;
import ao.co.kutatelamama.domain.entity.VaccinationRecord;
import ao.co.kutatelamama.domain.enums.VaccineStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface VaccinationRecordRepository extends JpaRepository<VaccinationRecord, Long> {
    List<VaccinationRecord> findByBabyOrderByScheduledDateAsc(Baby baby);
    List<VaccinationRecord> findByBabyIdOrderByScheduledDateAsc(Long babyId);
    List<VaccinationRecord> findByBabyAndStatus(Baby baby, VaccineStatus status);
    List<VaccinationRecord> findByScheduledDateBetweenAndStatus(LocalDate startDate, LocalDate endDate, VaccineStatus status);
}
