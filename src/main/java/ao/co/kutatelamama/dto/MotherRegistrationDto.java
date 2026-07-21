package ao.co.kutatelamama.dto;

import jakarta.validation.constraints.NotBlank;

public record MotherRegistrationDto(
    @NotBlank(message = "O número de telefone é obrigatório")
    String phoneNumber,

    @NotBlank(message = "O nome da mãe é obrigatório")
    String fullName,

    @NotBlank(message = "A província é obrigatória")
    String province,

    String municipality,

    @NotBlank(message = "O nome do bebé é obrigatório")
    String babyName,

    String babyGender,

    int babyAgeMonths
) {}
