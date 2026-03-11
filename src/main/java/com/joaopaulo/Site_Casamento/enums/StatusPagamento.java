package com.joaopaulo.Site_Casamento.enums;

import java.util.Arrays;

public enum StatusPagamento {
    PENDENTE("pending"),
    APROVADO("approved"),
    REJEITADO("rejected"),
    CANCELADO("cancelled"),
    REEMBOLSADO("refunded");

    private final String descricaoMp;

    StatusPagamento(String descricaoMp) {
        this.descricaoMp = descricaoMp;
    }

    public static StatusPagamento fromString(String status) {
        return Arrays.stream(StatusPagamento.values())
                .filter(s -> s.descricaoMp.equalsIgnoreCase(status) || s.name().equalsIgnoreCase(status))
                .findFirst()
                .orElse(PENDENTE);
    }
}