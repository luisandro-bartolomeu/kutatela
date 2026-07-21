package ao.co.kutatelamama.repository;

import ao.co.kutatelamama.domain.entity.Mother;
import ao.co.kutatelamama.domain.entity.TriageRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TriageRecordRepository extends JpaRepository<TriageRecord, Long> {
    List<TriageRecord> findByMotherOrderByCreatedAtDesc(Mother mother);
    List<TriageRecord> findByMotherIdOrderByCreatedAtDesc(Long motherId);
    List<TriageRecord> findAllByOrderByCreatedAtDesc();
}
