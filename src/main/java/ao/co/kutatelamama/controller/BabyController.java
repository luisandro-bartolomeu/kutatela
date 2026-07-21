package ao.co.kutatelamama.controller;

import ao.co.kutatelamama.domain.entity.Baby;
import ao.co.kutatelamama.repository.BabyRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/babies")
@CrossOrigin(origins = "*")
public class BabyController {

    private final BabyRepository babyRepository;

    public BabyController(BabyRepository babyRepository) {
        this.babyRepository = babyRepository;
    }

    @GetMapping
    public ResponseEntity<List<Baby>> getAllBabies() {
        return ResponseEntity.ok(babyRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Baby> getBabyById(@PathVariable Long id) {
        return babyRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/mother/{motherId}")
    public ResponseEntity<List<Baby>> getBabiesByMother(@PathVariable Long motherId) {
        return ResponseEntity.ok(babyRepository.findByMotherId(motherId));
    }
}
