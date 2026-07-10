package com.SistemaReservas.reservas_api.service;

import com.SistemaReservas.reservas_api.model.Usuario;
import com.SistemaReservas.reservas_api.model.enums.TipoUsuario;
import com.SistemaReservas.reservas_api.repository.ReservaRepository;
import com.SistemaReservas.reservas_api.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final ReservaRepository reservaRepository;

    public UsuarioService(UsuarioRepository repository, PasswordEncoder passwordEncoder, ReservaRepository reservaRepository) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.reservaRepository = reservaRepository;
    }

    public List<Usuario> listarTodos() {
        return repository.findAll();
    }

    public Usuario buscarPorId(Long id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
    }

    public Usuario cadastrar(Usuario usuario) {
        if (repository.existsByEmail(usuario.getEmail())) {
            throw new RuntimeException("Email já cadastrado");
        }

        // Gera senha temporária
        String senhaTemp = gerarSenhaAleatoria();
        usuario.setSenha(passwordEncoder.encode(senhaTemp));
        usuario.setSenhaTemporaria(true);

        if (usuario.getTipo() == null) {
            usuario.setTipo(TipoUsuario.COMUM);
        }

        Usuario salvo = repository.save(usuario);

        // Devolve a senha descriptografada uma vez para o admin anotar
        salvo.setSenha(senhaTemp);
        return salvo;
    }

    public Usuario editar(Long id, Usuario dados) {
        Usuario usuario = buscarPorId(id);
        usuario.setNome(dados.getNome());
        usuario.setEmail(dados.getEmail());
        usuario.setTelefone(dados.getTelefone());
        usuario.setDepartamento(dados.getDepartamento());
        usuario.setTipo(dados.getTipo());
        return repository.save(usuario);
    }

    public Usuario alterarAtivo(Long id, Boolean ativo) {
        Usuario usuario = buscarPorId(id);
        usuario.setAtivo(ativo);
        return repository.save(usuario);
    }

    public String resetarSenha(Long id) {
        Usuario usuario = buscarPorId(id);
        String senhaTemp = gerarSenhaAleatoria();
        usuario.setSenha(passwordEncoder.encode(senhaTemp));
        usuario.setSenhaTemporaria(true);
        repository.save(usuario);
        return senhaTemp;
    }

    private String gerarSenhaAleatoria() {
        String chars = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
        StringBuilder sb = new StringBuilder();
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    public void deletar(Long id) {
        Usuario usuario = buscarPorId(id);

        boolean temReservas = reservaRepository.existsByUsuarioId(usuario.getId());
        if (temReservas) {
            throw new RuntimeException("Este usuários possui reservas no histórico e não pode ser excluído. " + "Desative-o par aimpedir novos acessos");
        }

        repository.deleteById(id);
    }
}