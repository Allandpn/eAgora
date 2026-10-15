# PROGRESSO — App Android

Especificação: `especificacao/ESPECIFICACAO.md`. Decisões: `docs/ADR.md`.

| Fase | Entrega | Estado |
|---|---|---|
| A0 | Especificação, ADRs, ADR-041 e `02_JORNADAS` §2.2 no SGE | **feito** |
| A1 | Projeto Gradle, tema, configuração do servidor, cliente da API, Hoje, Recuperar, Registrar, envio otimista, rascunho persistente | **feito** |
| A2 | Assuntos, detalhe, Erros | **feito** |
| A3 | Progresso, Ajustes (com Lembrete e Servidor) | **feito** |
| A4 | Lembrete do turno, plano sem rede, CI com APK | **feito** (CI roda quando o repositório estiver no GitHub) |
| A5 | Uso em aparelho real (Tailscale + `tailscale serve`) | **você** |
| A6 | Sprint 18 do SGE: concurso-alvo em Hoje, disciplinas em Assuntos, Concursos, Edital novo por IA | **feito** — 34 testes (fixtures reais dos endpoints novos, consulta periódica, revisão e corpo de confirmação) |

## O que está verificado

- `./gradlew testDebugUnitTest assembleDebug assembleRelease` passando (2026-10-15).
- 29 testes: API contra **respostas reais do SGE** (capturadas rodando o servidor
  desta data, em `app/src/test/resources/sge/`), incluindo `problem+json`, erro sem
  JSON e falha de conexão; ViewModels de Hoje (drenar mais, plano sem rede),
  Recuperar (tempo de tela, rascunho, 24 h, mesmo `tentativaId` no reenvio, voltar
  sem registrar), Registrar (segmento sugerido, tempo pré-preenchido e não
  sobrescrito, validação de forma, corpo de questões) e endereço do servidor;
  Compose da regra vinculante de Recuperar; texto e horário do lembrete; abertura
  do app inteiro.

## O que falta conferir em aparelho (A5)

- [ ] Instalar o APK, configurar `https://<pi>.<tailnet>.ts.net` com a Tailscale ligada.
- [ ] Um turno real: Recuperar, Registrar um bloco, "Abrir material" abrindo o Drive.
- [ ] Matar o app no meio de uma recuperação (tirar dos recentes) e reabrir: a etapa volta.
- [ ] Ligar o lembrete para daqui a 2 minutos e ver a notificação.
- [ ] Modo avião em Hoje: aparece "Sem conexão — plano de HH:MM".

## Como este arquivo se mantém honesto

Fase só vira "feito" quando o build e os testes dela passam. Conferência em
aparelho real é registrada separadamente, porque o CI não tem emulador.
