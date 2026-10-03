package com.SistemaReservas.reservas_api.service;

import com.SistemaReservas.reservas_api.dto.request.UsuarioRequest;
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
    private final AuditoriaService auditoriaService;

    public UsuarioService(UsuarioRepository repository, PasswordEncoder passwordEncoder, ReservaRepository reservaRepository,
                          EmailService emailService, AuditoriaService auditoriaService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.reservaRepository = reservaRepository;
        this.emailService = emailService;
        this.auditoriaService = auditoriaService;
    }

    public List<Usuario> listarTodos() {
        return repository.findAll();
    }

    public Usuario buscarPorId(Long id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
    }

    public Usuario cadastrar(UsuarioRequest request) {
        if (repository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email já cadastrado");
        }

        Usuario usuario = new Usuario();
        usuario.setNome(request.getNome());
        usuario.setEmail(request.getEmail());
        usuario.setTelefone(request.getTelefone());
        usuario.setDepartamento(request.getDepartamento());
        usuario.setTipo(parseTipo(request.getTipo()));
        // ativo e senhaTemporaria não vêm do request: controlados só pelo backend.

        String senhaTemp = gerarSenhaAleatoria();
        usuario.setSenha(passwordEncoder.encode(senhaTemp));
        usuario.setSenhaTemporaria(true);

        Usuario salvo = repository.save(usuario);

        emailService.notificarNovoCadastro(
                salvo.getEmail(),
                salvo.getNome(),
                senhaTemp
        );

        return salvo;
    }

    public Usuario editar(Long id, UsuarioRequest dados) {
        Usuario usuario = buscarPorId(id);
        usuario.setNome(dados.getNome());
        usuario.setEmail(dados.getEmail());
        usuario.setTelefone(dados.getTelefone());
        usuario.setDepartamento(dados.getDepartamento());
        usuario.setTipo(parseTipo(dados.getTipo()));
        return repository.save(usuario);
    }

    private TipoUsuario parseTipo(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            return TipoUsuario.COMUM;
        }
        try {
            return TipoUsuario.valueOf(tipo);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Tipo de usuário inválido");
        }
    }

    public Usuario alterarAtivo(Long id, Boolean ativo) {
        Usuario usuario = buscarPorId(id);
        usuario.setAtivo(ativo);
        if (!Boolean.TRUE.equals(ativo)) {
            incrementarTokenVersion(usuario);
        }
        Usuario salvo = repository.save(usuario);
        auditoriaService.registrar(
                ativo ? "USUARIO_ATIVADO" : "USUARIO_DESATIVADO",
                "Usuário id=" + id + " (" + usuario.getEmail() + ")"
        );
        return salvo;
    }

    public String resetarSenha(Long id) {
        Usuario usuario = buscarPorId(id);
        String senhaTemp = gerarSenhaAleatoria();
        usuario.setSenha(passwordEncoder.encode(senhaTemp));
        usuario.setSenhaTemporaria(true);
        incrementarTokenVersion(usuario);
        repository.save(usuario);

        emailService.notificarSenhaResetada(
                usuario.getEmail(),
                usuario.getNome(),
                senhaTemp
        );

        auditoriaService.registrar("SENHA_RESETADA", "Usuário id=" + id + " (" + usuario.getEmail() + ")");

        return senhaTemp;
    }

    private void validarPoliticaSenha(String novaSenha) {
        if (novaSenha == null || novaSenha.length() < 8) {
            throw new RuntimeException("A nova senha deve ter pelo menos 8 caracteres");
        }
        boolean temLetra = novaSenha.chars().anyMatch(Character::isLetter);
        boolean temNumero = novaSenha.chars().anyMatch(Character::isDigit);
        if (!temLetra || !temNumero) {
            throw new RuntimeException("A nova senha deve conter letras e números");
        }
    }

    private static final java.security.SecureRandom SECURE_RANDOM = new java.security.SecureRandom();

    private String gerarSenhaAleatoria() {
        String chars = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            sb.append(chars.charAt(SECURE_RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }

    public void alterarSenha(String email, String senhaAtual, String novaSenha) {
        Usuario usuario = repository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        if (!passwordEncoder.matches(senhaAtual, usuario.getSenha())) {
            throw new RuntimeException("Senha atual incorreta");
        }

        validarPoliticaSenha(novaSenha);

        usuario.setSenha(passwordEncoder.encode(novaSenha));
        usuario.setSenhaTemporaria(false);
        incrementarTokenVersion(usuario);
        repository.save(usuario);
    }

    private void incrementarTokenVersion(Usuario usuario) {
        int atual = usuario.getTokenVersion() == null ? 0 : usuario.getTokenVersion();
        usuario.setTokenVersion(atual + 1);
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