# ADRs — App Android

Decisões de arquitetura do app. As decisões do sistema como um todo (rede,
autenticação, o app existir) estão no SGE: ADR-015, ADR-038, ADR-040, ADR-041.

| Campo | Significado |
|---|---|
| Status | `Aceita`, `Substituída por A-nnn`, `Revogada` |
| Regra | ADR aceita não é editada; mudou de ideia, escreva outra |

---

## A-001 — Kotlin + Jetpack Compose, um módulo só

**Status:** Aceita · 2026-10-15

**Contexto.** Um desenvolvedor, 3–5 h/semana, app pequeno (sete telas e uma
configuração).

**Decisão.** Kotlin, Compose com Material 3, um módulo Gradle `app`, pacotes
por camada (`ui`, `data`, `lembrete`).

**Consequências.** Build simples e rápido. Se o app crescer muito, separar
módulos depois é refatoração mecânica.

| Alternativa | Por que não |
|---|---|
| Views/XML | Mais código por tela, e o ecossistema novo é todo Compose |
| Multimódulo desde já | Custo de configuração sem ganho num app deste tamanho |

---

## A-002 — Retrofit + OkHttp + kotlinx.serialization

**Status:** Aceita · 2026-10-15

**Decisão.** Retrofit para declarar a API, OkHttp por baixo, kotlinx.serialization
com `ignoreUnknownKeys = true` e `explicitNulls = false`. A URL base vem da
configuração do servidor (ela muda em tempo de execução, então o cliente é
recriado quando o endereço muda).

**Consequências.** Campo novo do SGE não quebra o app (ADR-041 do SGE). O
OkHttp deixa um ponto de interceptação pronto para autenticação, se um dia
existir (ADR-040 do SGE).

| Alternativa | Por que não |
|---|---|
| Ktor client | Equivalente; Retrofit é o padrão que o dono encontra em vagas e tutoriais |
| Gson/Moshi | kotlinx.serialization é o caminho oficial do Kotlin e não usa reflexão |

---

## A-003 — Injeção de dependência manual

**Status:** Aceita · 2026-10-15

**Decisão.** Um `AppContainer` criado na `Application` monta API, repositórios
e agendador; ViewModels recebem o que precisam por fábrica.

| Alternativa | Por que não |
|---|---|
| Hilt/Dagger | Processamento de anotações, tempo de build e conceitos a mais para ~15 classes injetadas |
| Koin | Mais uma biblioteca para algo que um arquivo resolve |

---

## A-004 — ViewModel + StateFlow, sem regra de domínio

**Status:** Aceita · 2026-10-15

**Decisão.** Cada tela tem um `ViewModel` que expõe um `UiState` imutável num
`StateFlow`. O ViewModel só orquestra chamadas e formata; nenhuma regra D-xx
mora nele (ADR-041 do SGE). Formatação compartilhada (ex.: "Bloco 2 de 5 ·
p. 16–30 · ~60 min") mora em funções puras testáveis.

---

## A-005 — DataStore para configuração, rascunhos e cache do plano

**Status:** Aceita · 2026-10-15

**Decisão.** DataStore Preferences guarda: endereço do servidor, lembrete
(ligado, horário), rascunhos de Recuperar/Registrar (JSON por chave
`tipo:assunto`) e o último plano (JSON + hora).

| Alternativa | Por que não |
|---|---|
| Room | Banco relacional local para meia dúzia de chaves; e um banco local convida a duplicar domínio |
| SharedPreferences | API síncrona e legada; DataStore é a substituta oficial |

---

## A-006 — Envio otimista num escopo do aplicativo

**Status:** Aceita · 2026-10-15

**Contexto.** `02_JORNADAS` §5.1 do SGE exige transição otimista e falha não
destrutiva. Num app, sair da tela destrói o ViewModel dela.

**Decisão.** `EnvioRegistros` vive no `AppContainer` com um `CoroutineScope`
do aplicativo. A tela entrega o registro e volta para Hoje; o envio termina
mesmo sem a tela. Falhas ficam num `StateFlow` que Hoje observa.

**Consequência.** Se o processo morrer durante o envio, o rascunho continua
salvo (A-005) e o `tentativaId` garante que o reenvio não duplica (D-45).

---

## A-007 — WorkManager para o lembrete

**Status:** Aceita · 2026-10-15

**Decisão.** Trabalho periódico de 24 h com atraso inicial até o horário
escolhido; o worker consulta o plano e decide se notifica (`02_JORNADAS` §2.2).

| Alternativa | Por que não |
|---|---|
| AlarmManager exato | Exige permissão especial de alarme exato no Android 12+; precisão de minuto não importa para um lembrete |
| Push do servidor | Precisaria de Firebase e de o Pi falar com a internet, contra ADR-015 |

---

## A-008 — HTTPS obrigatório no release, HTTP só no debug

**Status:** Aceita · 2026-10-15

**Decisão.** O build de release só aceita endereço `https://` (o
`tailscale serve` fornece certificado válido). O build de debug declara
`usesCleartextTraffic` num manifesto próprio e aceita `http://`, para
desenvolver contra um servidor local.
