package ao.co.kutatelamama.controller;

import ao.co.kutatelamama.domain.entity.Mother;
import ao.co.kutatelamama.dto.MotherRegistrationDto;
import ao.co.kutatelamama.repository.MotherRepository;
import ao.co.kutatelamama.service.MotherService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/mothers")
@CrossOrigin(origins = "*")
public class MotherController {

    private final MotherRepository motherRepository;
    private final MotherService motherService;

    public MotherController(MotherRepository motherRepository, MotherService motherService) {
        this.motherRepository = motherRepository;
        this.motherService = motherService;
    }

    @GetMapping
    public ResponseEntity<List<Mother>> getAllMothers() {
        return ResponseEntity.ok(motherRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Mother> getMotherById(@PathVariable Long id) {
        return motherRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/phone/{phoneNumber}")
    public ResponseEntity<Mother> getMotherByPhone(@PathVariable String phoneNumber) {
        return motherService.findByPhoneNumber(phoneNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Mother> registerMother(@Valid @RequestBody MotherRegistrationDto dto) {
        Mother registered = motherService.registerMotherAndBaby(
                dto.phoneNumber(),
                dto.fullName(),
                dto.province(),
                dto.babyName(),
                dto.babyAgeMonths()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(registered);
    }
}
