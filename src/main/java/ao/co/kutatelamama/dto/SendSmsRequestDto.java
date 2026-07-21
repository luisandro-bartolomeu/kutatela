package ao.co.kutatelamama.dto;

import jakarta.validation.constraints.NotBlank;

public record SendSmsRequestDto(
    @NotBlank(message = "O telefone de destino é obrigatório")
    String recipientPhone,

    @NotBlank(message = "O tipo de mensagem é obrigatório")
    String messageType,

    @NotBlank(message = "O conteúdo da mensagem é obrigatório")
    String content
) {}
