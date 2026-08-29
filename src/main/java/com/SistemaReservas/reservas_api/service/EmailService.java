package com.SistemaReservas.reservas_api.service;

import com.SistemaReservas.reservas_api.model.Reserva;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${sistema.email.remetente}")
    private String remetente;

    @Value("${sistema.email.nome}")
    private String nomeRemetente;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // EMAIL GENÉRICO
    @Async
    private void enviar(String destinatario, String assunto, String corpo) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(remetente, nomeRemetente);
            helper.setTo(destinatario);
            helper.setSubject(assunto);
            helper.setText(corpo, true);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Erro ao enviar email: " + e.getMessage());
        }
    }

    // USUÁRIO CRIADO
    @Async
    public void notificarNovoCadastro(String emailDestinatario,
                                      String nomeUsuario,
                                      String senhaTemporaria) {
        String assunto = "Bem-vindo ao Sistema de Reservas — UniFil";
        String corpo = """
        <h2>Seu acesso foi criado!</h2>
        <p>Olá, <strong>%s</strong>.</p>
        <p>Você foi cadastrado no Sistema de Reservas de Salas da UniFil.</p>
        <p>Suas credenciais de acesso são:</p>
        <table style="margin:16px 0">
          <tr>
            <td><strong>Email:</strong></td>
            <td>%s</td>
          </tr>
          <tr>
            <td><strong>Senha temporária:</strong></td>
            <td style="font-family:monospace;font-size:18px;
                       font-weight:bold;letter-spacing:3px">%s</td>
          </tr>
        </table>
        <p>No seu primeiro acesso você será solicitado a criar uma senha pessoal.</p>
        <p>Acesse o sistema em: <a href="http://localhost:5173">Sistema de Reservas UniFil</a></p>
        <br><p>Sistema de Reservas — UniFil</p>
        """.formatted(nomeUsuario, emailDestinatario, senhaTemporaria);
        enviar(emailDestinatario, assunto, corpo);
    }

    // RESERVA CRIADA
    @Async
    public void notificarReservaCriada(Reserva reserva) {
        String assunto = "Solicitação de reserva recebida — " + reserva.getSalaNome();
        String corpo = """
            <h2>Solicitação recebida com sucesso!</h2>
            <p>Olá, <strong>%s</strong>.</p>
            <p>Sua solicitação de reserva foi registrada e está aguardando análise.</p>
            <table>
              <tr><td><strong>Sala:</strong></td><td>%s</td></tr>
              <tr><td><strong>Data:</strong></td><td>%s</td></tr>
              <tr><td><strong>Horário:</strong></td><td>%s às %s</td></tr>
              <tr><td><strong>Finalidade:</strong></td><td>%s</td></tr>
            </table>
            <p>Você será notificado quando houver uma decisão.</p>
            <br><p>Sistema de Reservas — UniFil</p>
            """.formatted(
                reserva.getNome(),
                reserva.getSalaNome(),
                reserva.getData().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                reserva.getHoraInicio().toString().substring(0, 5),
                reserva.getHoraFim().toString().substring(0, 5),
                reserva.getFinalidade()
        );
        enviar(reserva.getUsuarioEmail(), assunto, corpo);
    }

    // RESERVA APROVADA
    @Async
    public void notificarReservaAprovada(Reserva reserva) {
        String assunto = "Reserva aprovada — " + reserva.getSalaNome();
        String corpo = """
            <h2 style="color:#16a34a">Reserva aprovada!</h2>
            <p>Olá, <strong>%s</strong>.</p>
            <p>Sua reserva foi <strong>aprovada</strong>.</p>
            <table>
              <tr><td><strong>Sala:</strong></td><td>%s</td></tr>
              <tr><td><strong>Data:</strong></td><td>%s</td></tr>
              <tr><td><strong>Horário:</strong></td><td>%s às %s</td></tr>
            </table>
            <br><p>Sistema de Reservas — UniFil</p>
            """.formatted(
                reserva.getNome(),
                reserva.getSalaNome(),
                reserva.getData().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                reserva.getHoraInicio().toString().substring(0, 5),
                reserva.getHoraFim().toString().substring(0, 5)
        );
        enviar(reserva.getUsuarioEmail(), assunto, corpo);
    }

    // RESERVA REJEITADA
    @Async
    public void notificarReservaRejeitada(Reserva reserva) {
        String assunto = "Reserva não aprovada — " + reserva.getSalaNome();
        String corpo = """
            <h2 style="color:#dc2626">Reserva não aprovada</h2>
            <p>Olá, <strong>%s</strong>.</p>
            <p>Infelizmente sua reserva não foi aprovada.</p>
            <table>
              <tr><td><strong>Sala:</strong></td><td>%s</td></tr>
              <tr><td><strong>Data:</strong></td><td>%s</td></tr>
              <tr><td><strong>Horário:</strong></td><td>%s às %s</td></tr>
              <tr><td><strong>Motivo:</strong></td><td>%s</td></tr>
            </table>
            <p>Você pode realizar uma nova solicitação para outro horário ou sala.</p>
            <br><p>Sistema de Reservas — UniFil</p>
            """.formatted(
                reserva.getNome(),
                reserva.getSalaNome(),
                reserva.getData().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                reserva.getHoraInicio().toString().substring(0, 5),
                reserva.getHoraFim().toString().substring(0, 5),
                reserva.getMotivoRejeicao() != null ? reserva.getMotivoRejeicao() : "Não informado"
        );
        enviar(reserva.getUsuarioEmail(), assunto, corpo);
    }

    // MENSAGEM MANUAL DO GESTOR
    @Async
    public void enviarMensagemGestor(String destinatario, String nomeDestinatario,
                                     String mensagem, String nomeGestor) {
        String assunto = "Mensagem sobre sua reserva — Sistema UniFil";
        String corpo = """
            <p>Olá, <strong>%s</strong>.</p>
            <p>Você recebeu uma mensagem do gestor <strong>%s</strong>:</p>
            <blockquote style="border-left:3px solid #6366f1;padding-left:12px;color:#374151">
              %s
            </blockquote>
            <br><p>Sistema de Reservas — UniFil</p>
            """.formatted(nomeDestinatario, nomeGestor, mensagem);
        enviar(destinatario, assunto, corpo);
    }

    // SENHA RESETADA
    @Async
    public void notificarSenhaResetada(String emailDestinatario,
                                       String nomeUsuario,
                                       String senhaTemporaria) {
        String assunto = "Sua senha foi redefinida — Sistema de Reservas UniFil";
        String corpo = """
        <h2>Senha redefinida</h2>
        <p>Olá, <strong>%s</strong>.</p>
        <p>Sua senha de acesso ao Sistema de Reservas da UniFil foi redefinida por um administrador.</p>
        <p>Sua nova senha temporária é:</p>
        <div style="background:#f3f4f6;padding:16px;border-radius:8px;
                    font-size:24px;font-weight:bold;letter-spacing:4px;
                    text-align:center;font-family:monospace;margin:16px 0">
          %s
        </div>
        <p>Por segurança, você será solicitado a criar uma nova senha pessoal no seu próximo acesso.</p>
        <p>Se você não solicitou essa alteração, entre em contato com a administração imediatamente.</p>
        <br><p>Sistema de Reservas — UniFil</p>
        """.formatted(nomeUsuario, senhaTemporaria);
        enviar(emailDestinatario, assunto, corpo);
    }

    // CANCELAMENTO DE RESERVA POR SALA INDISPONÍVEL
    @Async
    public void notificarCancelamentoAdministrativo(Reserva reserva, String novoStatusSala) {
        String motivo = novoStatusSala.equals("MANUTENCAO")
                ? "a sala entrará em manutenção"
                : "a sala foi desativada";

        String assunto = "Reserva cancelada — " + reserva.getSalaNome();
        String corpo = """
        <h2 style="color:#dc2626">Reserva cancelada</h2>
        <p>Olá, <strong>%s</strong>.</p>
        <p>Sua reserva foi cancelada pela administração pois %s.</p>
        <table>
          <tr><td><strong>Sala:</strong></td><td>%s</td></tr>
          <tr><td><strong>Data:</strong></td><td>%s</td></tr>
          <tr><td><strong>Horário:</strong></td><td>%s às %s</td></tr>
        </table>
        <p>Por favor, realize uma nova solicitação para outro espaço disponível.</p>
        <br><p>Sistema de Reservas — UniFil</p>
        """.formatted(
                reserva.getNome(),
                motivo,
                reserva.getSalaNome(),
                reserva.getData().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                reserva.getHoraInicio().toString().substring(0, 5),
                reserva.getHoraFim().toString().substring(0, 5)
        );
        enviar(reserva.getUsuarioEmail(), assunto, corpo);
    }
}