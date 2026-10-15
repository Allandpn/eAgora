# SGE-Concursos — App Android

App Android nativo do [SGE-Concursos](https://github.com/Allandpn/SGE-Concursos):
plano do dia, recuperação espaçada, registro de estudo, erros, progresso e
ajustes — as mesmas telas da versão web, consumindo a mesma API no Raspberry Pi
pela Tailscale. A mais: lembrete diário do turno, rascunho que sobrevive ao
fechamento do app e "Abrir material" direto no Google Drive.

- Especificação: [`especificacao/ESPECIFICACAO.md`](especificacao/ESPECIFICACAO.md)
- Decisões de arquitetura: [`docs/ADR.md`](docs/ADR.md)
- Estado: [`PROGRESSO.md`](PROGRESSO.md)

## Instalar no celular

1. Tailscale instalado e ligado no celular, na mesma tailnet do Pi.
2. No Pi, o SGE publicado com `sudo tailscale serve --bg 8080` (HTTPS).
3. Baixe o APK (artefato do GitHub Actions ou release) e instale, permitindo
   "instalar apps desconhecidos" para o app que abriu o arquivo.
4. Na primeira abertura, informe o endereço `https://<nome-do-pi>.<tailnet>.ts.net`.

## Desenvolver

```
export ANDROID_HOME=/caminho/do/android-sdk   # platforms;android-35, build-tools;35.0.0
./gradlew testDebugUnitTest assembleDebug
```
