package com.joaopaulo.Site_Casamento.controller;

import com.joaopaulo.Site_Casamento.service.CodigoVerificacaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/validacao")
@RequiredArgsConstructor
public class CodigoVerificacaoController {

    private final CodigoVerificacaoService codigoVerificacaoService;

    @PostMapping("/enviar-codigo")
    public ResponseEntity<Map<String, String>> enviar(@RequestBody Map<String, String> request) {
        return ResponseEntity.ok(codigoVerificacaoService.gerarEnviarCodigo(request.get("email")));
    }

    @PostMapping("/verificar-codigo")
    public ResponseEntity<Map<String, Object>> verificar(@RequestBody Map<String, String> request) {
        return ResponseEntity.ok(codigoVerificacaoService.validarCodigo(request.get("email"), request.get("codigo")));
    }
}