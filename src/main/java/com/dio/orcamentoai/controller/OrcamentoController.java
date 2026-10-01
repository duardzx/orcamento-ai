package com.dio.orcamentoai.controller;

import com.dio.orcamentoai.model.Transacao;
import com.dio.orcamentoai.service.OrcamentoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/orcamento")
public class OrcamentoController {

    private final OrcamentoService orcamentoService;

    public OrcamentoController(OrcamentoService orcamentoService) {
        this.orcamentoService = orcamentoService;
    }

    @PostMapping("/audio")
    public ResponseEntity<Transacao> processarAudio(@RequestParam("file") MultipartFile file) {
        Transacao transacaoSalva = orcamentoService.processarAudioEGravarTransacao(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(transacaoSalva);
    }

    @GetMapping("/transacoes")
    public ResponseEntity<List<Transacao>> listarTransacoes() {
        return ResponseEntity.ok(orcamentoService.listarTodas());
    }
}