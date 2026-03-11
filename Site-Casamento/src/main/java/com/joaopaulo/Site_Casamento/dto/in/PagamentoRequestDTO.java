package com.joaopaulo.Site_Casamento.dto.in;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PagamentoRequestDTO(
        @NotNull BigDecimal valor,
        @Email String email,
        @NotBlank String nome,
        String descricao
        ) {
}
