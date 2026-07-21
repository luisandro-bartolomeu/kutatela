package ao.co.kutatelamama.repository;

import ao.co.kutatelamama.domain.entity.Vaccine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VaccineRepository extends JpaRepository<Vaccine, Long> {
    Optional<Vaccine> findByName(String name);
    List<Vaccine> findByRecommendedAgeMonths(Integer recommendedAgeMonths);
    List<Vaccine> findAllByOrderByRecommendedAgeMonthsAsc();
}
