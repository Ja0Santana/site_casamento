package com.joaopaulo.Site_Casamento.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "codigo_verificacao", indexes = {
        @Index(name = "idx_email_validacao", columnList = "email")
})
public class CodigoVerificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(nullable = false, length = 5)
    private String codigo;

    @Column(nullable = false)
    private LocalDateTime dataExpiracao;
}