package ao.co.kutatelamama.repository;

import ao.co.kutatelamama.domain.entity.WeeklyTip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WeeklyTipRepository extends JpaRepository<WeeklyTip, Long> {
    Optional<WeeklyTip> findByWeekNumber(Integer weekNumber);
    List<WeeklyTip> findByCategory(String category);
    List<WeeklyTip> findAllByOrderByWeekNumberAsc();
}
