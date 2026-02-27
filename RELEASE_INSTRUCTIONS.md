# Instruções para Release - Amigo Secreto

## 1. Gerar Keystore (Primeira vez apenas)

Execute o comando abaixo para criar seu keystore:

```bash
keytool -genkey -v -keystore amigosecreto.keystore -alias amigosecreto -keyalg RSA -keysize 2048 -validity 10000
```

**IMPORTANTE:**
- Guarde a senha do keystore em local seguro
- Nunca commit o keystore no git
- Faça backup do keystore em local seguro (se perder, não poderá mais atualizar o app na Play Store)

## 2. Configurar Signing no build.gradle

Edite o arquivo `app/build.gradle` e descomente as linhas na seção `signingConfigs.release`:

```gradle
signingConfigs {
    release {
        storeFile file("../amigosecreto.keystore")
        storePassword "SUA_SENHA_AQUI"
        keyAlias "amigosecreto"
        keyPassword "SUA_SENHA_AQUI"
    }
}
```

E descomente também no buildTypes.release:

```gradle
release {
    signingConfig signingConfigs.release
    // ... resto da configuração
}
```

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

- [ ] Versão atualizada no build.gradle (versionCode e versionName)
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
- **versionCode**: Número inteiro incremental (atual: 8)
- **versionName**: Versão legível (atual: 2.0)

Incrementar a cada nova versão:
- **Patch** (2.0.1): Correções de bugs
- **Minor** (2.1.0): Novas funcionalidades
- **Major** (3.0.0): Mudanças significativas

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

## 10. Suporte

Para problemas ou dúvidas, verifique:
- Logs de build: `./gradlew bundleRelease --stacktrace`
- Documentação: https://developer.android.com/studio/publish
