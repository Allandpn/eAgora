# Instruções para o Claude neste repositório

App Android nativo do **SGE-Concursos** (Kotlin + Jetpack Compose). É o segundo
cliente da mesma API REST; a SPA web é o primeiro (ADR-041 do SGE).

## A regra que vale mais que todas

**O app não tem regra de domínio.** Fase, escada, plano do turno, resultado,
consolidação: tudo vem do servidor. Se uma tela precisa de algo que a API não
diz, o certo é mudar a API no SGE (documento primeiro, lá), nunca calcular aqui.

## Onde está cada coisa

| Definir… | Dono |
|---|---|
| Jornadas, telas, regras vinculantes, textos das âncoras, lembrete | `SGE-Concursos/especificacao/02_JORNADAS.md` |
| Regra de domínio D-xx | `SGE-Concursos/especificacao/01_DOMINIO.md` |
| O que o app consome da API, navegação, rascunho, notificação, build | `especificacao/ESPECIFICACAO.md` (este repositório) |
| Decisão de arquitetura do app | `docs/ADR.md` |
| Estado | `PROGRESSO.md` |

Contradição entre este repositório e o SGE é defeito daqui: corrija o
documento deste repositório.

## Antes de mexer em algo que toca a API

Confira o código atual do SGE (`github.com/Allandpn/SGE-Concursos`): DTOs em
`src/main/java/br/com/estudos/**/**Response.java` e `**Request.java`,
endpoints nos `*Controller.java`. A tabela de §4 da especificação lista o que
o app usa — atualize-a no mesmo commit.

## Rodar

```
./gradlew testDebugUnitTest      # testes JVM (inclui Robolectric)
./gradlew assembleDebug          # APK em app/build/outputs/apk/debug/
```

Precisa de `ANDROID_HOME` apontando para um SDK com `platforms;android-35` e
`build-tools;35.0.0`.

## Ao terminar uma tarefa

- [ ] build e testes passando;
- [ ] `PROGRESSO.md` atualizado;
- [ ] especificação atualizada se algo divergiu — documento primeiro;
- [ ] nenhuma regra de domínio entrou no app.
