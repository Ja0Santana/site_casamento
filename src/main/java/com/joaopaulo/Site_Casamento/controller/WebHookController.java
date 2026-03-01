package com.joaopaulo.Site_Casamento.controller;

import com.joaopaulo.Site_Casamento.service.NotificacaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/webhooks")
@RequiredArgsConstructor
public class WebHookController {

    private final NotificacaoService notificacaoService;

    @PostMapping("/mercado-pago")
    public ResponseEntity<Void> receberNotificacao(@RequestBody Map<String, Object> payload) {
        notificacaoService.receberNotificacao(payload);
        return ResponseEntity.ok().build();
    }
}