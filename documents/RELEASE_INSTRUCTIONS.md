# Instruções para Release - Amigo Secreto

## 1. Gerar Keystore (Primeira vez apenas)

### Duas chaves, não uma

O app é publicado como App Bundle (`.aab`), o que exige **Play App Signing**. Existem, portanto,
duas chaves distintas — e é importante não confundi-las:

| Chave | Quem guarda | O que assina |
|-------|-------------|--------------|
| Chave de assinatura do app | Google | os APKs que o Play gera a partir do bundle e entrega aos usuários |
| **Chave de upload** (o keystore deste projeto) | Você | o `.aab` enviado ao Play, apenas para autenticar o upload |

O keystore gerado abaixo é a **chave de upload**.

### Gerar

```bash
keytool -genkey -v -keystore amigosecreto.keystore -alias amigosecreto -keyalg RSA -keysize 2048 -validity 10000
```

**IMPORTANTE:**

- Guarde a senha do keystore em local seguro.
- Nunca commite o keystore no git — o `.gitignore` já cobre `*.keystore`, `amigosecreto.keystore`
  e `keystore.properties`.
- Faça backup do keystore. Perder a chave de upload **é recuperável**, mas custa tempo: é preciso
  solicitar um *reset da chave de upload* no Play Console, gerar um keystore novo e registrar o
  novo certificado. Enquanto o reset não é processado, não é possível publicar atualizações.
- A chave de assinatura do app **não** pode ser perdida por você — ela fica sob custódia do Google.
  Por isso trocar de keystore de upload não quebra as atualizações para quem já instalou o app.

## 2. Configurar Signing (local)

O `app/build.gradle` **já está configurado** — não há nada para descomentar. Ele lê as credenciais
de duas fontes, nesta ordem de precedência:

1. `keystore.properties` na raiz do projeto (desenvolvimento local)
2. Variáveis de ambiente `CI_KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` (CI)

**Nunca** coloque senha diretamente no `app/build.gradle` — o arquivo é versionado.

### Desenvolvimento local

Crie `keystore.properties` na raiz do repositório (já coberto pelo `.gitignore`):

```properties
storeFile=amigosecreto.keystore
storePassword=<senha do keystore>
keyAlias=amigosecreto
keyPassword=<senha da chave>
```

`storeFile` é resolvido via `rootProject.file(...)`, ou seja, o caminho é relativo à raiz do
repositório — não ao diretório `app/`.

Se `keystore.properties` não existir e nenhuma variável de ambiente de CI estiver presente, o build
de release sai **sem** `signingConfig` (`signingConfig canSign ? signingConfigs.release : null`).
O `.aab` gerado fica não assinado e não serve para upload no Play.

### CI (GitHub Actions)

Os workflows decodificam o secret `KEYSTORE_BASE64` para um arquivo temporário e exportam
`CI_KEYSTORE_PATH`; as demais credenciais vêm dos secrets `KEYSTORE_PASSWORD`, `KEY_ALIAS` e
`KEY_PASSWORD`. Nenhuma configuração local é necessária para o deploy automatizado.

## 3. Gerar App Bundle para Play Store

Para gerar o App Bundle otimizado:

```bash
./gradlew bundleRelease
```

O arquivo será gerado em:
```
app/build/outputs/bundle/release/app-release.aab
```

## 4. Testar Release Build Localmente

Para gerar APK de release:

```bash
./gradlew assembleRelease
```

O APK será gerado em:
```
app/build/outputs/apk/release/app-release.apk
```

## 5. Checklist antes de publicar

- [ ] Tag de release criada com o formato correto (ex: `v3.1`) — versionCode e versionName são calculados automaticamente pelo CI (ver seção 7)
- [ ] Testado em diferentes dispositivos e versões do Android
- [ ] Ícone do app configurado
- [ ] Screenshots preparados (mínimo 2, máximo 8)
- [ ] Descrição do app em português
- [ ] Política de privacidade publicada (veja PRIVACY_POLICY.md)
- [ ] Permissões justificadas na Play Console
- [ ] Build assinado com keystore de produção

## 6. Upload na Google Play Console

1. Acesse: https://play.google.com/console
2. Selecione seu app ou crie um novo
3. Vá em "Release" > "Production"
4. Faça upload do arquivo .aab
5. Preencha as notas de versão
6. Revise e publique

## 7. Versioning

O projeto usa semantic versioning:
- **versionCode**: Calculado automaticamente: `100 + git rev-list --count HEAD` (produção ~157+)
- **versionName**: Extraído da tag de release (ex: tag `v3.0` → versionName `3.0`)

Não é necessário editar versionCode ou versionName manualmente — o CI faz isso via workflow.

Incrementar a cada nova versão:
- **Patch** (3.0.1): Correções de bugs
- **Minor** (3.1.0): Novas funcionalidades
- **Major** (4.0.0): Mudanças significativas

## 8. Tamanho do App

O build está otimizado com:
- ProGuard/R8 habilitado (minification e obfuscation)
- Resource shrinking habilitado
- App Bundle splits por densidade e ABI
- Vector drawables otimizados

## 9. Segurança

O app implementa:
- Network Security Config (apenas HTTPS)
- ProGuard/R8 obfuscation
- Backup rules configuradas
- Sem logs em release builds
- FileProvider para compartilhamento seguro

## 10. Atualização de dependências de build

### KSP (Kotlin Symbol Processing)

A versão do KSP no `build.gradle` raiz segue o formato `<kotlin_version>-<ksp_release>`:

```gradle
classpath "com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:${kotlin_version}-1.0.28"
```

**Ao bumpar `kotlin_version`**, verificar a versão KSP compatível em:
https://github.com/google/ksp/releases

A versão KSP deve ter o mesmo prefixo da versão Kotlin. Exemplo:
- `kotlin_version = '2.0.21'` → KSP `2.0.21-1.0.28`
- `kotlin_version = '2.1.0'`  → verificar última release KSP com prefixo `2.1.0-x.x.xx`

Se o prefixo KSP não corresponder ao Kotlin, o build falha com erro de version mismatch.

## 11. Suporte

Para problemas ou dúvidas, verifique:
- Logs de build: `./gradlew bundleRelease --stacktrace`
- Documentação: https://developer.android.com/studio/publish
