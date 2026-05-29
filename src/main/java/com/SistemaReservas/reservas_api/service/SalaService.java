package com.SistemaReservas.reservas_api.service;

import com.SistemaReservas.reservas_api.model.Sala;
import com.SistemaReservas.reservas_api.repository.SalaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SalaService {

    private final SalaRepository repository;

    public SalaService(SalaRepository repository) {
        this.repository = repository;
    }

    public List<Sala> listarTodas() {
        return repository.findAll();
    }

    public List<Sala> listarDisponiveis() {
        return repository.findByStatus("ATIVO");
    }

    public Sala buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sala não encontrada"));
    }

    public Sala salvar(Sala sala) {
        // Valida duplicata apenas no cadastro
        if (sala.getId() == null &&
                repository.existsByNomeAndBloco(sala.getNome(), sala.getBloco())) {
            throw new RuntimeException("Já existe uma sala com este nome neste bloco");
        }
        return repository.save(sala);
    }

    public Sala alterarStatus(Long id, String status) {
        Sala sala = buscarPorId(id);
        sala.setStatus(status);
        return repository.save(sala);
    }

    public void deletar(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Sala não encontrada");
        }
        repository.deleteById(id);
    }
}