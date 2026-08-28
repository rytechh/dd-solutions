package br.com.rytechh.greenshift.adapters.in.rest.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;

public record CreateJobRequest(

        @NotBlank(message = "comando é obrigatório")
        String comando,

        String descricao,

        @Positive(message = "duracaoMinutos deve ser maior que zero")
        int duracaoMinutos,

        @NotNull(message = "deadline é obrigatório")
        @Future(message = "deadline deve estar no futuro")
        Instant deadline,

        Double consumoEstimadoKw
) {
}
