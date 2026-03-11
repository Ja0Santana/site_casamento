package com.joaopaulo.Site_Casamento.mapper;

import com.joaopaulo.Site_Casamento.dto.out.PagamentoResponseDTO;
import com.joaopaulo.Site_Casamento.model.Pagamento;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PagamentoMapper {

    @Mapping(target = "id", source = "pagamento.mercadoPagoId")
    @Mapping(target = "statusPagamento", source = "pagamento.statusPagamento")
    @Mapping(target = "valor", source = "pagamento.valor")
    @Mapping(target = "nome", source = "pagamento.nome")
    @Mapping(target = "descricao", source = "pagamento.descricao")
    @Mapping(target = "qrCode", source = "qrCode")
    @Mapping(target = "qrCode64", expression = "java(\"data:image/png;base64,\" + qrCode64)")
    PagamentoResponseDTO toResponseDTO(Pagamento pagamento, String qrCode, String qrCode64);
}