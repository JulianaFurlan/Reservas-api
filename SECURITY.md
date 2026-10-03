# Segurança — Sistema de Reservas UniFil

Este documento registra o hardening de segurança aplicado ao backend e o que ainda
precisa de atenção manual (rotação de segredos, deploy, frontend).

## 1. Segredos e variáveis de ambiente

`application.properties` não contém mais nenhum segredo: todos os valores sensíveis
vêm de variáveis de ambiente (`${VAR}`), sem valor padrão, para a aplicação falhar ao
subir se algo não for configurado (em vez de usar um fallback inseguro).

Variáveis obrigatórias em qualquer ambiente:

| Variável       | Descrição                                      |
|----------------|-------------------------------------------------|
| `DB_PASSWORD`  | Senha do MySQL                                   |
| `JWT_SECRET`   | Segredo HS256, mínimo 32 bytes (256 bits)        |
| `MAIL_USERNAME`| Usuário SMTP                                      |
| `MAIL_PASSWORD`| Senha/senha de app SMTP                           |

Opcionais (têm default): `DB_URL`, `DB_USERNAME`, `SERVER_PORT`, `MAIL_HOST`,
`MAIL_PORT`, `MAIL_FROM`, `MAIL_FROM_NAME`, `JWT_EXPIRATION`, `CORS_ALLOWED_ORIGINS`,
`REQUIRE_HTTPS`.

**Para desenvolvimento local:** copie
`src/main/resources/application-local.properties.example` para
`application-local.properties` (mesma pasta — já está no `.gitignore`), preencha os
valores reais e rode com `SPRING_PROFILES_ACTIVE=local`.

**Para produção:** defina as variáveis de ambiente na plataforma de deploy (Railway,
Render, systemd unit, container orchestrator etc.). Nunca em arquivo versionado.

### Ação pendente: segredos que já circularam

- A senha de app do Gmail que estava hardcoded (`fdyt dykd xidm puug`) foi exposta em
  texto puro no working tree. **Revogue essa senha de app em
  https://myaccount.google.com/apppasswords e gere uma nova** antes de usar o sistema
  em produção, mesmo que ela não tenha chegado a ser commitada.
- O JWT secret antigo (`chaveSuperSecretaParaJWTComPeloMenos32Caracteres...`) e a senha
  de banco `123456` já estavam commitados no histórico do GitHub. Foram rotacionados
  no código; se o histórico do repositório também foi reescrito (ver commit/PR que
  acompanha esta mudança), os commits antigos já não existem mais no remoto.

## 2. HTTPS / HSTS

- `security.require-https=true` (variável `REQUIRE_HTTPS`) ativa um filtro
  (`HttpsRedirectFilter`) que redireciona HTTP → HTTPS. Deixe `false` em
  desenvolvimento local.
- O header `Strict-Transport-Security` já é enviado automaticamente pelo Spring
  Security em qualquer resposta servida sobre HTTPS (`max-age` de 1 ano, incluindo
  subdomínios).
- **Isso não substitui TLS real.** Em produção, a aplicação deve ficar atrás de um
  proxy reverso (nginx, load balancer da plataforma de deploy, Cloudflare etc.) com um
  certificado TLS válido (Let's Encrypt é suficiente) — é essa camada que normalmente
  deve fazer o redirecionamento HTTP→HTTPS. `server.forward-headers-strategy=framework`
  já está configurado para a aplicação confiar corretamente no header
  `X-Forwarded-Proto` enviado pelo proxy.

## 3. Token JWT em localStorage (dívida técnica conhecida)

O token continua sendo devolvido no corpo do JSON de login e guardado no
`localStorage` do frontend (decisão explícita: não migrar agora para cookie HttpOnly).

**Risco aceito:** se o frontend tiver uma vulnerabilidade de XSS (injeção de script),
o token pode ser roubado via `localStorage.getItem`, permitindo ao atacante agir como
o usuário até o token expirar.

**Mitigações já em vigor que reduzem o impacto:**
- Expiração do JWT reduzida de 24h para 1h (`jwt.expiration`).
- `tokenVersion`: trocar a senha, resetar senha ou desativar a conta invalida
  imediatamente qualquer token antigo daquele usuário (ver seção 5).
- CSP (`Content-Security-Policy: default-src 'self'`) dificulta a execução de scripts
  de origens externas.

**Migração futura recomendada:** mover o token para cookie HttpOnly + Secure +
SameSite, setado pelo backend no login. Quando isso for feito, o backend deixa de
devolver o token no JSON e passa a ler o cookie nos endpoints protegidos; no frontend,
remove-se o uso de `localStorage` para o token e adiciona-se `withCredentials: true`
no `axios.create()`, e o CORS do backend já está pronto para isso (`allowCredentials`
já é `true` com origem explícita, nunca `*`).

## 4. CORS

Origem permitida configurável via `CORS_ALLOWED_ORIGINS` (lista separada por vírgula),
default `http://localhost:5173`. Em produção, defina essa variável com a URL exata do
frontend (ex.: `https://reservas.unifil.br`). Métodos e headers permitidos foram
restringidos ao necessário (`GET,POST,PUT,DELETE,OPTIONS`; headers
`Authorization,Content-Type`).

## 5. Rate limiting / brute force

`LoginAttemptService` bloqueia por 15 minutos após 5 falhas consecutivas, tanto por
email quanto por IP de origem. É em memória (não sobrevive a restart nem escala para
múltiplas instâncias) — suficiente para o volume de uma aplicação institucional de
porte pequeno/médio; se o sistema crescer para múltiplas instâncias, trocar por um
contador compartilhado (Redis) seria o próximo passo.

## 6. Headers HTTP de segurança

Adicionados via Spring Security: `X-Frame-Options: DENY`,
`X-Content-Type-Options: nosniff`, `Content-Security-Policy`, `Referrer-Policy`,
`Strict-Transport-Security` (quando HTTPS). O header `Server` é zerado em todas as
respostas (`ServerHeaderFilter`) para não anunciar a tecnologia do servidor.

## 7-9. Validação server-side, mass assignment e exposição de dados

- Conflito de horário, antecedência mínima (10 min) e máxima (1 ano) para reservas são
  validados no `ReservaService`, independente do que o frontend envia.
- `UsuarioRequest`/`ReservaRequest` substituíram o bind direto das entidades JPA nos
  controllers: campos como `status` (reserva), `tipo`/`ativo`/`senhaTemporaria`
  (usuário) e `usuarioId` nunca vêm do corpo da requisição do lado de quem não deveria
  poder setá-los.
- `/api/usuarios` e `/api/usuarios/{id}` agora respondem com `UsuarioResponse` (sem o
  hash da senha); `/api/auth/login` já fazia isso.
- `GlobalExceptionHandler` centraliza erros: mensagens de negócio conhecidas chegam ao
  cliente, mas qualquer exceção inesperada (erro de banco, NPE etc.) retorna uma
  mensagem genérica e é logada só no servidor.
- IDOR corrigido: ver/editar/cancelar uma reserva agora exige ser o dono ou
  GESTOR/ADMIN (antes, qualquer usuário autenticado podia ver, aprovar, rejeitar ou
  cancelar a reserva de qualquer outra pessoa só sabendo o ID).

## 10. Expiração e revogação de token

- `jwt.expiration` default de 1h (era 24h). Não há refresh token implementado — ao
  expirar, o usuário precisa logar de novo. Se isso incomodar a experiência de uso,
  implementar um endpoint de refresh token é o próximo passo natural.
- Campo `tokenVersion` em `Usuario`: incrementado ao trocar senha, resetar senha ou
  desativar a conta. O token carrega essa versão como claim (`tv`) e
  `JwtAuthenticationFilter` rejeita qualquer token cuja versão não bata com a atual no
  banco — ou seja, trocar a senha invalida instantaneamente todos os tokens antigos,
  mesmo que ainda não tenham expirado.

## 11. Logs

Nenhuma senha em texto puro, token JWT completo ou segredo é escrito em log. Senhas
temporárias geradas agora usam `SecureRandom` (antes usava `java.util.Random`, que não
é adequado para gerar segredos).

## 12. Dependências

`mvn dependency:tree` confirma que o projeto já está em versões bem recentes (Spring
Boot 4.0.5 / Spring Framework 7 / Spring Security 7.0.4 / Hibernate 7.2.7 / MySQL
Connector/J 9.6.0), geridas pelo BOM do Spring Boot. A única dependência presa
manualmente em versão antiga é `io.jsonwebtoken:jjwt-*:0.11.5` (atual é 0.12.x); não
há CVE crítica conhecida nela, mas vale atualizar quando houver tempo para revisar a
API (ela muda entre 0.11 e 0.12). No frontend, rode `npm audit` e corrija os achados
de severidade alta/crítica.

## 13. Auditoria

Tabela `registros_auditoria` (`RegistroAuditoria`/`AuditoriaService`) registra quem
aprovou/rejeitou/reverteu reservas e quem resetou ou desativou a conta de quem, com
data/hora. Consulta via `GET /api/auditoria` (somente ADMIN).

---

## Checklist para replicar no frontend

- [ ] Nada muda no formato do login nem no uso de `localStorage` (decisão: manter por
      ora — ver seção 3).
- [ ] Senha: atualizar validação do campo de nova senha de mínimo 6 para **mínimo 8
      caracteres, com letra e número** (o backend agora rejeita senhas mais fracas).
- [ ] Tratar respostas `401` do login com a mensagem genérica "Email ou senha
      inválidos" (não há mais distinção entre "usuário não existe" e "senha errada").
- [ ] Tratar resposta `429` do login ("Muitas tentativas de login...") — mostrar a
      mensagem do backend ao usuário em vez de um erro genérico.
- [ ] O token agora expira em 1h (era 24h): garantir que a aplicação trata `401` em
      qualquer chamada autenticada redirecionando para o login (se ainda não fizer
      isso).
- [ ] Ações de aprovar/rejeitar/reverter/cancelar reserva e ver detalhes de uma reserva
      que não é do próprio usuário agora retornam `403` se quem chama não é o dono
      nem GESTOR/ADMIN — garantir que a UI não tente mostrar essas ações para quem não
      tem permissão (evita um 403 "surpresa").
- [ ] Se a URL do frontend em produção for diferente de `http://localhost:5173`,
      configurar a variável de ambiente `CORS_ALLOWED_ORIGINS` no backend com essa URL
      exata.
