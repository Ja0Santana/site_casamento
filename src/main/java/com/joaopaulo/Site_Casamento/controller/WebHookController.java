package com.joaopaulo.Site_Casamento.controller;

import com.joaopaulo.Site_Casamento.service.PagamentoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/webhooks")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "https://casamentoelenejoaopaulo.men")
public class WebHookController {

    private final PagamentoService pagamentoService;

    @SuppressWarnings("unchecked")
    @PostMapping("/mercado-pago")
    public ResponseEntity<Void> receberNotificacao(@RequestBody Map<String, Object> payload) {
        // 1. Log discreto para você saber que algo chegou, sem poluir tudo
        log.info("Webhook recebido: type={}, action={}", payload.get("type"), payload.get("action"));

        String type = (String) payload.get("type");

        // 2. Filtro simplificado: Se não for sobre 'payment', ignore.
        // Não filtre pela 'action' aqui, pois ela pode variar (created, updated, etc)
        if (!"payment".equals(type)) {
            return ResponseEntity.ok().build();
        }

        Map<String, Object> data = (Map<String, Object>) payload.get("data");

        if (data != null && data.get("id") != null) {
            try {
                Long mpId = Long.valueOf(data.get("id").toString());
                // O Service vai consultar a API e ver o status real (pendente ou aprovado)
                pagamentoService.atualizarStatusPagamento(mpId, payload);
            } catch (Exception e) {
                log.error("Erro ao processar ID {}: {}", data.get("id"), e.getMessage());
            }
        }

        return ResponseEntity.ok().build();
    }
}