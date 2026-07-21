package ao.co.kutatelamama.repository;

import ao.co.kutatelamama.domain.entity.Baby;
import ao.co.kutatelamama.domain.entity.Mother;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BabyRepository extends JpaRepository<Baby, Long> {
    List<Baby> findByMother(Mother mother);
    List<Baby> findByMotherId(Long motherId);
    Optional<Baby> findFirstByMotherIdOrderByCreatedAtDesc(Long motherId);
}
