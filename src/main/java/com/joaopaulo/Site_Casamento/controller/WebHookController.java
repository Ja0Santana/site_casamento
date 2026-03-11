package com.joaopaulo.Site_Casamento.controller;

import com.joaopaulo.Site_Casamento.service.PagamentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/webhooks")
@RequiredArgsConstructor
public class WebHookController {

    private final PagamentoService pagamentoService;

    @PostMapping("/mercado-pago")
    public ResponseEntity<Void> receberNotificacao(@RequestBody Map<String, Object> payload) {
        pagamentoService.receberNotificacao(payload);
        return ResponseEntity.ok().build();
    }
}