# Especificação — App Android do SGE-Concursos

| Campo | Valor |
|---|---|
| Versão | 1.0.0 |
| Data | 2026-10-15 |
| Status | Vigente |
| Subordinada a | `SGE-Concursos`: `especificacao/02_JORNADAS.md` v1.8.0 (jornadas, telas, regras vinculantes, §2.2 lembrete), `docs/00A_ADR.md` ADR-015, ADR-038, ADR-040, ADR-041, `docs/04_FRONTEND.md` (comportamento de referência das telas) |

---

## 0. O que este app é, e o que ele não é

**É** o segundo cliente do SGE-Concursos (ADR-041 do SGE): as mesmas sete telas
da SPA, nativas no Android, consumindo a mesma API REST no Raspberry Pi pela
Tailscale. Acrescenta três coisas que o navegador faz mal:

1. **Lembrete diário do turno** (`02_JORNADAS` §2.2), sem servidor de push.
2. **Rascunho que sobrevive** ao Android fechar o app no meio de um registro.
3. **Abrir o material** de um bloco direto no app do Google Drive.

**Não é:**

- Um lugar de regra de domínio. Fase, escada, resultado de questões, plano do
  turno, consolidação: tudo vem do servidor. O app não calcula nada disso.
  Se uma tela precisaria decidir algo que o servidor não diz, o certo é a API
  passar a dizer, não o app inventar.
- Um cliente offline. Ler o último plano sem rede é permitido (§5.3);
  registrar exige o servidor. Fila de envio offline continua fora
  (`02_JORNADAS` §5.1: "maquinaria real... vira decisão própria").
- Um app de loja. Distribuição por APK instalado manualmente (§8).

### 0.1 Precedência

| Assunto | Onde está definido |
|---|---|
| Jornadas, telas, textos de botões das regras vinculantes, orçamentos de tempo, lembrete | `02_JORNADAS` do SGE — **este documento não redefine** |
| Regra de domínio (D-xx) | `01_DOMINIO` do SGE — o app nunca implementa |
| Formato da API | Código do SGE (DTOs) e §4 deste documento, que espelha o que o app consome |
| Navegação, persistência local, notificação, build, distribuição | Este documento |

Contradição entre este documento e o SGE é defeito deste documento.

---

## 1. Plataforma

| Item | Valor | Por quê |
|---|---|---|
| Linguagem | Kotlin | Padrão do Android; o dono já lê Java |
| UI | Jetpack Compose + Material 3 | UI declarativa; menos código que Views |
| `minSdk` | 26 (Android 8.0) | `java.time` nativo sem desugaring; cobre praticamente todo aparelho em uso |
| `targetSdk` / `compileSdk` | 35 | Exigência atual do Android para apps novos |
| Aparelhos | Celular e tablet, retrato e paisagem | Mesmo layout de coluna única, largura máxima de 640 dp centrada em tela larga |
| Idioma | Português (Brasil) | Textos em `strings.xml` |
| Tema | Claro e escuro, seguindo o sistema | Cores da paleta da SPA (`04_FRONTEND` §8A): primária `#0694A2`, alerta `#E2530A` |

---

## 2. Arquitetura

Decisões com o porquê em `docs/ADR.md`. Resumo:

```
ui/<tela>/      Tela (Composable) + ViewModel (StateFlow de um UiState)
data/api/       Retrofit + OkHttp + kotlinx.serialization: SgeApi e DTOs
data/           Repositórios: Configuracao (DataStore), Rascunhos (DataStore),
                PlanoCache (DataStore), EnvioRegistros (envio otimista, §5.2)
lembrete/       WorkManager: agendamento e worker da notificação (§6)
AppContainer    Injeção manual de dependências — sem Hilt
```

- **Uma Activity**, navegação com Navigation Compose. Rotas:
  `hoje`, `recuperar/{assuntoId}`, `registrar/{tipo}/{assuntoId}`, `assuntos`,
  `assuntos/{assuntoId}`, `erros/{assuntoId}`, `progresso`, `ajustes`,
  `configuracao`.
- Barra inferior com Hoje, Assuntos, Progresso e Ajustes. Recuperar, Registrar,
  detalhe de assunto e Erros abrem por cima, com voltar.
- **Handoff entre telas pelos argumentos da rota mais um repositório em
  memória** (`Selecao`): a SPA usa *stores* porque a API não tem
  `GET /api/assuntos/{id}`; aqui o item escolhido (nome, nível, data prevista,
  segmento sugerido) vai num objeto em memória, e a rota leva só o id. Abrir a
  rota sem o objeto mostra o estado "sem dados", igual à SPA.

---

## 3. As telas

Cada tela segue a jornada e o comportamento já especificados no SGE. Aqui só
o que muda por ser Android.

### 3.1 Hoje (J-2, J-4)

Mesmas seções e ordem da SPA (`04_FRONTEND` §6, §7G, §7I): mensagem de retorno
(com `diasSemEstudar ≥ 2`), nota de represamento (sem a mensagem de retorno),
Recuperar, Reestudar, bloco de conteúdo ("Continuar leitura" ou "Conteúdo
novo", com bloco N de M e "Abrir material"), "Quer adiantar?" no fim.

- **Abrir material** dispara `Intent.ACTION_VIEW` com o link; o Android oferece
  o app do Google Drive. Link que não é `http(s)` não mostra o botão.
- **Puxar para atualizar** recarrega o plano.
- **Sem rede:** mostra o último plano guardado com a faixa "Sem conexão —
  plano de HH:MM" (§5.3). Os itens continuam tocáveis; o registro falha de
  forma não destrutiva.
- Falha de um envio otimista (§5.2) aparece como *snackbar* em Hoje com
  "Reenviar", que reabre a tela de origem com o rascunho intacto.

### 3.2 Recuperar (J-2, `02_JORNADAS` §4.1)

As regras vinculantes valem sem exceção:

- **Etapa 1 não contém o resultado** — o Composable da etapa 2 não existe na
  árvore enquanto a etapa é 1 (não é escondido, é ausente).
- Etapa 2 mostra a previsão como texto, não editável.
- Sem botão "pular". Voltar do sistema sai sem registrar.
- Textos e âncoras exatamente os de §4.1 do SGE.
- `tempoMinutos` = tempo da tela aberta (abrir → confirmar), mínimo 1 — a
  recuperação continua medida pela tela (`02_JORNADAS` v1.7.0).
- "+ registrar um erro" na etapa 2 abre Erros do assunto.

### 3.3 Registrar sessão (J-2, `04_FRONTEND` §7A, §7G, §7H)

Pílulas Conteúdo / Questões / Flashcards; campos e regras da SPA:

- Conteúdo: bloco lido (lista dos segmentos; "O assunto inteiro (começa as
  revisões)" como primeira opção; segmento sugerido já marcado), tempo
  pré-preenchido com `tempoEstimadoMin` do bloco ou `duracao_bloco_min`; depois
  que o usuário edita, nada sobrescreve.
- Questões: formato, feitas, certas, previsão (%), tempo (vazio).
- Flashcards: feitas, certas, previsão (%), tempo (vazio).
- Validação local só de forma (número inteiro ≥ 1 no tempo); regra de domínio
  é do servidor, e o erro dele (`detail`) é mostrado como veio.

### 3.4 Assuntos e detalhe (J-1, J-3, `04_FRONTEND` §7B)

Lista por disciplina com ordem, nome, peso e fase (uma chamada de fase por
assunto, como a SPA). Detalhe: disciplina, peso, fase, ações Conteúdo /
Questões / Flashcards / Erros, material (cada segmento com páginas, tempo e
"abrir"), retenção (M-2).

### 3.5 Erros (J-2, J-3, `04_FRONTEND` §7C)

Formulário (o que aconteceu, causa, confiança) e lista do assunto. **Não é
otimista**, de propósito: espera o POST, limpa e recarrega a lista.

### 3.6 Progresso (J-3, `04_FRONTEND` §7D, §7I)

Balanço semanal na ordem de J-3: Eu me conheço? (M-3), Estou melhorando?
(M-1, pílulas Global / Por disciplina, "—" abaixo do `n`), A frente está
saudável?, assuntos que podem estar grandes demais (D-35), Estou em dia? (M-4).

### 3.7 Ajustes (`04_FRONTEND` §7E)

Os parâmetros do servidor, agrupados (Tetos, Turno, Limiares de resultado,
Escada), com os mesmos rótulos da SPA; cada um salva sozinho ao confirmar
(`PATCH`), sem otimismo; erro volta o valor anterior e mostra o motivo.

No fim da tela, duas entradas que são só do app: **Lembrete** (§6) e
**Servidor** (§3.8).

### 3.8 Configuração do servidor (só do app)

Primeira abertura: pede o endereço do SGE (ex.:
`https://raspberrypi.tail1234.ts.net`) e testa com `GET /actuator/health`.

- Em build de **release**, só `https://` é aceito (HTTPS obrigatório,
  `03E_DEPLOYMENT` §8A do SGE).
- Em build de **debug**, `http://` é aceito, para desenvolvimento contra um
  servidor local.
- O endereço fica em DataStore; trocar exige novo teste.

---

## 4. A API consumida

O app consome exatamente estes endpoints do SGE. Os DTOs ficam em
`data/api/Dtos.kt`, com os mesmos nomes de campo.

| Tela | Chamada |
|---|---|
| Configuração | `GET /actuator/health` |
| Hoje, lembrete | `GET /api/turno/plano?extraRecuperacoes=N` |
| Recuperar, Registrar | `POST /api/sessoes` |
| Registrar | `GET /api/assuntos/{id}/segmentos`, `GET /api/parametros` (só `duracao_bloco_min`) |
| Assuntos | `GET /api/disciplinas`, `GET /api/assuntos?disciplinaId=`, `GET /api/assuntos/{id}/fase` |
| Detalhe | `GET /api/assuntos/{id}/segmentos`, `GET /api/metricas/m2?assuntoId=` |
| Erros | `GET /api/erros?assuntoId=`, `POST /api/erros` |
| Progresso | `GET /api/metricas/m1?janela=`, `GET /api/metricas/m3`, `GET /api/metricas/m4`, `GET /api/frente`, `GET /api/frente/sugestoes-divisao`, `GET /api/disciplinas` |
| Ajustes | `GET /api/parametros`, `PATCH /api/parametros/{chave}` |

**Compatibilidade:**

- JSON com `ignoreUnknownKeys`: campo novo do servidor não quebra o app.
- Campo que o app lê e o servidor deixar de mandar quebra — é mudança de API
  que exige versão nova do app (ADR-041 do SGE).
- Erro do servidor é `application/problem+json` com `detail`, `codigo`, `campo`
  (ADR-026 do SGE). Resposta de erro sem esse formato vira "O servidor
  respondeu com erro (N)". Falha de conexão vira "Sem conexão com o servidor".

---

## 5. Falha, rascunho e o que funciona sem rede

### 5.1 Identidade da tentativa (D-45)

O `tentativaId` (UUID) nasce **ao abrir** Recuperar ou Registrar e fica no
rascunho. Reenvio usa o mesmo id: o servidor devolve o registro já gravado em
vez de duplicar. É isso que torna seguro reenviar depois de uma resposta
perdida.

### 5.2 Envio otimista, falha não destrutiva (`02_JORNADAS` §5.1)

- Ao confirmar, a tela volta para Hoje na hora; o envio continua num escopo do
  aplicativo (não da tela), então sair da tela não o cancela.
- Sucesso: o rascunho é apagado e Hoje recarrega.
- Falha: o rascunho continua guardado e Hoje mostra a *snackbar* "Não foi
  possível registrar — Reenviar", que reabre a tela com tudo preenchido.

### 5.3 Rascunho persistente

- Recuperar e Registrar gravam o rascunho em DataStore a cada mudança de
  campo, por assunto e tipo, com o `tentativaId`.
- Se o Android matar o app, reabrir a mesma tela do mesmo assunto recupera o
  rascunho — inclusive a etapa da recuperação e a previsão já dada.
- Rascunho de recuperação guarda o instante em que a tela abriu, para o tempo
  medido continuar honesto.
- Rascunho com mais de 24 h é descartado ao abrir (o dia do registro mudou).

### 5.4 Leitura sem rede

O último plano carregado com sucesso fica em DataStore com a hora. Sem rede,
Hoje mostra esse plano com a faixa de aviso. Nenhuma outra tela tem cache.

---

## 6. Lembrete do turno

Regras de produto: `02_JORNADAS` §2.2 do SGE. Implementação:

- **Desligado por padrão.** Ligar pede a permissão de notificação (Android 13+).
- Horário escolhido pelo usuário (padrão 19:00). Agendado com WorkManager:
  trabalho periódico de 24 h, com o primeiro disparo calculado para o horário
  escolhido. O horário é aproximado (o Android pode adiar alguns minutos) —
  aceitável para um lembrete.
- No disparo, o worker chama `GET /api/turno/plano`:
  - `diasSemEstudar == 0` (já estudou hoje): **não notifica**;
  - com resposta: "Hoje: N recuperações e 1 bloco de leitura." (só as partes
    que existem; com reestudo, "N para reestudar");
  - sem resposta: "Hora do turno de estudo." — sem números, sem alarme.
- Tocar na notificação abre Hoje.
- Canal de notificação próprio ("Lembrete do turno"), importância padrão.

---

## 7. Testes

| Camada | Como |
|---|---|
| API e DTOs | `MockWebServer`: o app desserializa respostas reais do SGE (copiadas da execução de 2026-10-15) e trata `problem+json`, erro sem JSON e falha de conexão |
| ViewModels | JVM, `kotlinx-coroutines-test`, API falsa: estados de carregando/erro/dados, envio otimista e falha, rascunho, `extraRecuperacoes` |
| Regras vinculantes de Recuperar | Teste de Compose com Robolectric: o resultado não existe na etapa 1 |
| Lembrete | Teste do texto da notificação a partir do plano, e da regra "já estudou hoje" |
| Build | `./gradlew assembleDebug testDebugUnitTest` no CI a cada push |

Não há teste instrumentado em aparelho no CI (sem emulador no runner). A
conferência em aparelho real é do usuário (§8).

---

## 8. Build e distribuição

- Gradle com *wrapper*; `./gradlew assembleDebug` gera
  `app/build/outputs/apk/debug/app-debug.apk`.
- GitHub Actions: a cada push, roda testes e publica o APK como artefato.
- **Assinatura:** para instalar uma versão por cima da outra, o APK precisa da
  mesma assinatura. O CI assina com a chave guardada em segredos do
  repositório (`ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`,
  `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`); sem os segredos, gera APK de
  debug (cada máquina assina diferente — trocar de origem exige desinstalar).
- Instalação: baixar o APK no celular, permitir "instalar apps desconhecidos"
  para o navegador ou o gerenciador de arquivos, instalar. Tailscale ligado no
  aparelho.

---

## 9. Changelog

| Versão | Data | Mudança |
|---|---|---|
| 1.0.0 | 2026-10-15 | Criado, a pedido do usuário, junto com a ADR-041 e `02_JORNADAS` v1.8.0 do SGE |
