package ao.co.kutatelamama.controller;

import ao.co.kutatelamama.domain.entity.Baby;
import ao.co.kutatelamama.domain.entity.Mother;
import ao.co.kutatelamama.domain.entity.TriageRecord;
import ao.co.kutatelamama.dto.SymptomTriageRequestDto;
import ao.co.kutatelamama.repository.MotherRepository;
import ao.co.kutatelamama.repository.TriageRecordRepository;
import ao.co.kutatelamama.service.MotherService;
import ao.co.kutatelamama.service.TriageAiService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/triages")
@CrossOrigin(origins = "*")
public class TriageController {

    private final TriageRecordRepository triageRecordRepository;
    private final TriageAiService triageAiService;
    private final MotherRepository motherRepository;
    private final MotherService motherService;

    public TriageController(TriageRecordRepository triageRecordRepository,
                            TriageAiService triageAiService,
                            MotherRepository motherRepository,
                            MotherService motherService) {
        this.triageRecordRepository = triageRecordRepository;
        this.triageAiService = triageAiService;
        this.motherRepository = motherRepository;
        this.motherService = motherService;
    }

    @GetMapping
    public ResponseEntity<List<TriageRecord>> getAllTriages() {
        return ResponseEntity.ok(triageRecordRepository.findAllByOrderByCreatedAtDesc());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TriageRecord> getTriageById(@PathVariable Long id) {
        return triageRecordRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/mother/{motherId}")
    public ResponseEntity<List<TriageRecord>> getTriagesByMother(@PathVariable Long motherId) {
        return ResponseEntity.ok(triageRecordRepository.findByMotherIdOrderByCreatedAtDesc(motherId));
    }

    @PostMapping
    public ResponseEntity<TriageRecord> performTriage(@Valid @RequestBody SymptomTriageRequestDto dto) {
        Mother mother = motherRepository.findByPhoneNumber(dto.phoneNumber()).orElse(null);
        Baby baby = mother != null ? motherService.getOrCreateDefaultBabyForMother(mother) : null;

        TriageRecord record = triageAiService.performTriage(mother, baby, dto.category(), dto.detail());
        return ResponseEntity.status(HttpStatus.CREATED).body(record);
    }
}
