# Checklist de Produção - Amigo Secreto

Use este checklist antes de publicar na Google Play Store:

## 📋 Preparação do Código

- [x] Versão atualizada no `build.gradle` (versionCode: 8, versionName: 2.0)
- [x] ProGuard/R8 habilitado e configurado
- [x] Resource shrinking habilitado
- [x] Logs de debug removidos (via ProGuard)
- [x] Permissões declaradas e justificadas
- [x] Network security config configurado (apenas HTTPS)
- [x] Backup rules configuradas
- [x] FileProvider configurado
- [ ] Código testado em dispositivos reais
- [ ] Testado em diferentes versões do Android (API 21-34)

## 🔐 Segurança

- [x] ProGuard rules implementadas
- [x] Obfuscação habilitada
- [x] Network security config (sem cleartext)
- [x] Dados sensíveis não hardcoded
- [ ] Keystore criado e guardado em local seguro
- [ ] Senha do keystore documentada (fora do git)
- [x] .gitignore atualizado para não commitar keystores

## 🎨 Recursos Visuais

- [x] Ícone do app criado (adaptive icon)
- [x] Ícone monocromático para Android 13+
- [x] Ícone round criado
- [ ] Screenshots capturados (mínimo 2, recomendado 8)
- [ ] Feature graphic criado (1024x500)
- [ ] Vídeo promocional criado (opcional)

## 📱 Testes

- [ ] Testado em telefones (pequeno, médio, grande)
- [ ] Testado em tablets
- [ ] Testado rotação de tela
- [ ] Testado com diferentes fontes/tamanhos
- [ ] Testado modo escuro/claro
- [ ] Testado permissões (aceitar e negar)
- [ ] Testado sem conexão internet
- [ ] Testado instalação limpa
- [ ] Testado atualização de versão anterior
- [ ] Testado backup e restore

## 📝 Documentação

- [x] README.md atualizado
- [x] PRIVACY_POLICY.md criado
- [x] RELEASE_INSTRUCTIONS.md criado
- [x] PLAY_STORE_LISTING.md preparado
- [ ] Política de privacidade publicada online
- [ ] Link da política adicionado no Play Store listing

## 🏗️ Build

- [x] Debug build testado
- [x] Release build testado (sem signing)
- [ ] Release build com signing testado
- [ ] APK instalado e testado manualmente
- [ ] App Bundle gerado (.aab)
- [ ] Tamanho do APK/Bundle verificado

## 📦 Google Play Console

- [ ] Conta de desenvolvedor criada ($25 único)
- [ ] App criado na Play Console
- [ ] Informações básicas preenchidas:
  - [ ] Título do app
  - [ ] Descrição curta
  - [ ] Descrição completa
  - [ ] Categoria selecionada
- [ ] Store listing preenchido:
  - [ ] Screenshots carregados
  - [ ] Ícone carregado
  - [ ] Feature graphic carregado
  - [ ] Vídeo adicionado (opcional)
- [ ] Política de privacidade:
  - [ ] URL da política publicada
  - [ ] Link adicionado no app listing
- [ ] Classificação de conteúdo:
  - [ ] Questionário preenchido
  - [ ] Classificação obtida
- [ ] Preço e distribuição:
  - [ ] Países selecionados
  - [ ] Preço definido (grátis)
  - [ ] Termos aceitos
- [ ] Data Protection (Android 12+):
  - [ ] Práticas de privacidade declaradas
  - [ ] Tipos de dados coletados especificados

## 🚀 Release

- [ ] App Bundle (.aab) carregado
- [ ] Notas de versão preenchidas
- [ ] Track de release selecionado:
  - [ ] Internal testing (primeiro teste)
  - [ ] Closed testing (beta teste)
  - [ ] Open testing (beta público)
  - [ ] Production (lançamento final)
- [ ] Revisão iniciada
- [ ] Aguardando aprovação do Google (1-7 dias)

## 📊 Pós-Lançamento

- [ ] Monitorar crashes (Play Console)
- [ ] Verificar reviews dos usuários
- [ ] Responder feedback
- [ ] Monitorar métricas de uso
- [ ] Planejar próximas atualizações

## 🔧 Comandos Úteis

### Gerar Keystore (primeira vez)
```bash
keytool -genkey -v -keystore amigosecreto.keystore -alias amigosecreto -keyalg RSA -keysize 2048 -validity 10000
```

### Build Debug
```bash
./gradlew assembleDebug
```

### Build Release (precisa configurar signing)
```bash
./gradlew assembleRelease
```

### Gerar App Bundle (recomendado para Play Store)
```bash
./gradlew bundleRelease
```

### Limpar Build
```bash
./gradlew clean
```

### Ver Tamanho do APK
```bash
./gradlew assembleRelease && ls -lh app/build/outputs/apk/release/
```

## 📞 Suporte

- **Documentação Android**: https://developer.android.com/studio/publish
- **Play Console**: https://play.google.com/console
- **Políticas do Google Play**: https://play.google.com/about/developer-content-policy/

## ⚠️ IMPORTANTE

1. **NUNCA commit o keystore no git**
2. **Faça backup do keystore em 3 locais diferentes**
3. **Documente a senha em local seguro (gerenciador de senhas)**
4. **Teste tudo antes de publicar**
5. **Leia as políticas do Google Play**

## 🎯 Status Atual

**Versão**: 2.0 (versionCode 8)
**Data de preparação**: 26/02/2024
**Pronto para produção**: ✅ (após criar keystore e testar)

---

**Próximos passos**:
1. Criar keystore de produção
2. Capturar screenshots
3. Publicar política de privacidade online
4. Criar conta de desenvolvedor
5. Gerar App Bundle assinado
6. Upload na Play Console
