package com.joaopaulo.Site_Casamento.repository;

import com.joaopaulo.Site_Casamento.model.Pagamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {
    Optional<Pagamento> findByMercadoPagoId(Long mercadoPagoId);

    List<Pagamento> findAllByStatusPagamento(com.joaopaulo.Site_Casamento.enums.StatusPagamento status);
}
