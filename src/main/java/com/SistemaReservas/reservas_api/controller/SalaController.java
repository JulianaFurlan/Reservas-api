package com.SistemaReservas.reservas_api.controller;

import com.SistemaReservas.reservas_api.model.Sala;
import com.SistemaReservas.reservas_api.service.SalaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/salas")
@CrossOrigin(origins = "http://localhost:5173")
public class SalaController {

    private final SalaService service;

    public SalaController(SalaService service) {
        this.service = service;
    }

    // Qualquer usuário logado pode ver salas disponíveis
    @GetMapping
    public List<Sala> listar() {
        return service.listarTodas();
    }

    @GetMapping("/disponiveis")
    public List<Sala> listarDisponiveis() {
        return service.listarDisponiveis();
    }

    // Somente admin
    @PostMapping
    public ResponseEntity<Sala> criar(@RequestBody Sala sala) {
        return ResponseEntity.status(201).body(service.salvar(sala));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Sala> atualizar(@PathVariable Long id,
                                          @RequestBody Sala sala) {
        sala.setId(id);
        return ResponseEntity.ok(service.salvar(sala));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Sala> alterarStatus(@PathVariable Long id,
                                              @RequestBody java.util.Map<String, String> body) {
        return ResponseEntity.ok(service.alterarStatus(id, body.get("status")));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}