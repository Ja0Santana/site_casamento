package com.joaopaulo.Site_Casamento.repository;

import com.joaopaulo.Site_Casamento.model.CodigoVerificacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface CodigoVerificacaoRepository extends JpaRepository<CodigoVerificacao, Long> {
    Optional<CodigoVerificacao> findByEmail(String email);
    void deleteByDataExpiracaoBefore(LocalDateTime data);
    void deleteByEmail(String email);
}