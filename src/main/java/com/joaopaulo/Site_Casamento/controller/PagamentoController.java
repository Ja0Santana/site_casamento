package com.joaopaulo.Site_Casamento.controller;

import com.joaopaulo.Site_Casamento.dto.in.PagamentoRequestDTO;
import com.joaopaulo.Site_Casamento.dto.out.PagamentoResponseDTO;
import com.joaopaulo.Site_Casamento.service.PagamentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/pagamentos")
@RequiredArgsConstructor
public class PagamentoController {

    private final PagamentoService pagamentoService;

    @GetMapping("/key")
    public ResponseEntity<Map<String, String>> getPublicKey() {
        return ResponseEntity.ok(Map.of("key: ", pagamentoService.obterPublicKey()));
    }

    @PostMapping("/pix")
    public ResponseEntity<PagamentoResponseDTO> criarPix(@Valid @RequestBody PagamentoRequestDTO pagamentoRequestDTO) {
        PagamentoResponseDTO response = pagamentoService.criarPagamentoPix(pagamentoRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/card")
    public ResponseEntity<Map<String, Object>> processarPagamentoCard(@RequestBody Map<String, Object> payload) {
        pagamentoService.processarPagamentoCard(payload);
        return ResponseEntity.ok(Map.of("status", "success"));
    }

    @GetMapping("/reprocessar-pendentes")
    public ResponseEntity<Map<String, String>> reprocessar() {
        pagamentoService.verificarPagamentosPendentes();
        return ResponseEntity.ok(Map.of("message", "Processamento iniciado"));
    }
}
