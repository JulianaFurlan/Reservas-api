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
    private final EmailService emailService;

    public UsuarioService(UsuarioRepository repository, PasswordEncoder passwordEncoder, ReservaRepository reservaRepository,  EmailService emailService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.reservaRepository = reservaRepository;
        this.emailService = emailService;
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

        String senhaTemp = gerarSenhaAleatoria();
        usuario.setSenha(passwordEncoder.encode(senhaTemp));
        usuario.setSenhaTemporaria(true);

        if (usuario.getTipo() == null) {
            usuario.setTipo(TipoUsuario.COMUM);
        }

        Usuario salvo = repository.save(usuario);

        emailService.notificarNovoCadastro(
                salvo.getEmail(),
                salvo.getNome(),
                senhaTemp
        );

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

        emailService.notificarSenhaResetada(
                usuario.getEmail(),
                usuario.getNome(),
                senhaTemp
        );

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

    public void alterarSenha(String email, String senhaAtual, String novaSenha) {
        Usuario usuario = repository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        if (!passwordEncoder.matches(senhaAtual, usuario.getSenha())) {
            throw new RuntimeException("Senha atual incorreta");
        }

        if (novaSenha.length() < 6) {
            throw new RuntimeException("A nova senha deve ter pelo menos 6 caracteres");
        }

        usuario.setSenha(passwordEncoder.encode(novaSenha));
        usuario.setSenhaTemporaria(false);
        repository.save(usuario);
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