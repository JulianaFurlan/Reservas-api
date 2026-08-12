package com.SistemaReservas.reservas_api.controller;

import com.SistemaReservas.reservas_api.model.Sala;
import com.SistemaReservas.reservas_api.model.enums.StatusSala;
import com.SistemaReservas.reservas_api.service.SalaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/salas")
@CrossOrigin(origins = "http://localhost:5173")
public class SalaController {

    private final SalaService service;

    public SalaController(SalaService service) {
        this.service = service;
    }

    @GetMapping
    public List<Sala> listar() {
        return service.listarTodas();
    }

    // Qualquer usuário logado pode ver salas disponíveis
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
    public ResponseEntity<Sala> atualizar(@PathVariable Long id, @RequestBody Sala sala) {
        sala.setId(id);
        return ResponseEntity.ok(service.salvar(sala));
    }

    @GetMapping("/status/opcoes")
    public ResponseEntity<List<Map<String, String>>> getStatusOptions() {
        List<Map<String, String>> options = Arrays.stream(StatusSala.values())
                .map(status -> {
                    Map<String, String> map = new HashMap<>();
                    map.put("value", status.name());
                    return map;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(options);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Sala> alterarStatus(@PathVariable Long id, @RequestBody java.util.Map<String, String> body) {
        return ResponseEntity.ok(service.alterarStatus(id, body.get("status")));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}