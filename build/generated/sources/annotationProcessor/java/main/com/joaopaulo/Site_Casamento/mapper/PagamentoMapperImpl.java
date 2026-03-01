package com.joaopaulo.Site_Casamento.mapper;

import com.joaopaulo.Site_Casamento.dto.out.PagamentoResponseDTO;
import com.joaopaulo.Site_Casamento.enums.StatusPagamento;
import com.joaopaulo.Site_Casamento.model.Pagamento;
import java.math.BigDecimal;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-03-01T15:01:04-0300",
    comments = "version: 1.5.5.Final, compiler: IncrementalProcessingEnvironment from gradle-language-java-8.14.4.jar, environment: Java 21.0.9 (Microsoft)"
)
@Component
public class PagamentoMapperImpl implements PagamentoMapper {

    @Override
    public PagamentoResponseDTO toResponseDTO(Pagamento pagamento, String qrCode, String qrCode64) {
        if ( pagamento == null && qrCode == null && qrCode64 == null ) {
            return null;
        }

        Long id = null;
        StatusPagamento statusPagamento = null;
        BigDecimal valor = null;
        String nome = null;
        String descricao = null;
        if ( pagamento != null ) {
            id = pagamento.getMercadoPagoId();
            statusPagamento = pagamento.getStatusPagamento();
            valor = pagamento.getValor();
            nome = pagamento.getNome();
            descricao = pagamento.getDescricao();
        }
        String qrCode1 = null;
        qrCode1 = qrCode;

        String qrCode64_1 = "data:image/png;base64," + qrCode64;

        PagamentoResponseDTO pagamentoResponseDTO = new PagamentoResponseDTO( id, statusPagamento, valor, nome, qrCode1, qrCode64_1, descricao );

        return pagamentoResponseDTO;
    }
}
