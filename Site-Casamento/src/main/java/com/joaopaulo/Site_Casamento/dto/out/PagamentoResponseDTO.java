package com.joaopaulo.Site_Casamento.dto.out;

import com.joaopaulo.Site_Casamento.enums.StatusPagamento;

import java.math.BigDecimal;

public record PagamentoResponseDTO(Long id,
                                   StatusPagamento statusPagamento,
                                   BigDecimal valor,
                                   String nome,
                                   String qrCode,
                                   String qrCode64,
                                   String descricao) {}
