package ao.co.kutatelamama.repository;

import ao.co.kutatelamama.domain.entity.SmsLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SmsLogRepository extends JpaRepository<SmsLog, Long> {
    List<SmsLog> findByRecipientPhoneOrderBySentAtDesc(String recipientPhone);
    List<SmsLog> findAllByOrderBySentAtDesc();
}
