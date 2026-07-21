package ao.co.kutatelamama.dto;

import ao.co.kutatelamama.domain.enums.SymptomCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SymptomTriageRequestDto(
    @NotBlank(message = "O número de telefone da mãe é obrigatório")
    String phoneNumber,

    @NotNull(message = "A categoria do sintoma é obrigatória")
    SymptomCategory category,

    @NotBlank(message = "O detalhe do sintoma é obrigatório")
    String detail
) {}
