package com.joaopaulo.Site_Casamento.model;

import com.joaopaulo.Site_Casamento.enums.StatusPagamento;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "pagamentos") // Boa prática: evita conflito com palavras reservadas
public class Pagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true) // Garante que não teremos registros duplicados do mesmo pagamento MP
    private Long mercadoPagoId;

    @Column(precision = 10, scale = 2) // Melhor prática para valores monetários
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    private StatusPagamento statusPagamento;

    private String email;
    private String nome;

    @Column(columnDefinition = "TEXT") // Essencial, pois o QR Code Base64 é uma string gigantesca
    private String qrCode;

    private String dataPagamento;

    // Campo adicionado para os e-mails
    @Column(length = 500) // Descrições podem ser um pouco longas
    private String descricao;
}