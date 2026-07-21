package ao.co.kutatelamama.repository;

import ao.co.kutatelamama.domain.entity.Mother;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MotherRepository extends JpaRepository<Mother, Long> {
    Optional<Mother> findByPhoneNumber(String phoneNumber);
    boolean existsByPhoneNumber(String phoneNumber);
}
