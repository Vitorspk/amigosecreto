# Amigo Secreto - Sorteio e SMS

Aplicativo Android para organizar sorteios de amigo secreto com compartilhamento via WhatsApp e SMS.

Disponivel na Google Play Store: `com.amigosecreto.sorteio`

## Funcionalidades

- Criar multiplos grupos para diferentes eventos
- Adicionar participantes manualmente ou importando da agenda de contatos
- Definir restricoes (quem nao pode tirar quem)
- Sorteio automatico com validacao de restricoes e transacao atomica
- Revelacao interativa do resultado com protecao anti-spoiler
- Compartilhamento via WhatsApp (com 30 linhas em branco anti-spoiler) e SMS
- Lista de desejos por participante: produto, categoria, faixa de preco, lojas sugeridas
- Busca de produtos via Buscape integrada

## Stack

- **Linguagem**: Java 17
- **Min SDK**: 21 (Android 5.0) / **Target SDK**: 35 (Android 15)
- **Banco de dados**: SQLite (schema v8, via `MySQLiteOpenHelper`)
- **UI**: Material Design 3 com suporte a Edge-to-Edge (Android 15)
- **Build**: Android Gradle Plugin 9.0.1, ViewBinding, R8/ProGuard em release

## Estrutura

```
app/src/main/java/activity/amigosecreto/
├── GruposActivity.java              # LAUNCHER - tela principal
├── ParticipantesActivity.java       # gerenciar participantes e sorteio
├── RevelarAmigoActivity.java        # revelar resultado interativamente
├── ParticipanteDesejosActivity.java # desejos de um participante
├── VisualizarDesejosActivity.java   # todos os desejos de um grupo
├── ListarDesejos.java               # lista de desejos geral
├── InserirDesejoActivity.java       # adicionar desejo
├── AlterarDesejoActivity.java       # editar desejo
├── DetalheDesejoActivity.java       # detalhes do desejo + busca Buscape
├── adapter/
│   └── ParticipantesRecyclerAdapter.java
├── db/
│   ├── MySQLiteOpenHelper.java      # schema SQLite v8
│   ├── Grupo.java / GrupoDAO.java
│   ├── Participante.java / ParticipanteDAO.java
│   └── Desejo.java / DesejoDAO.java
└── util/
    ├── AnimationUtils.java
    ├── AsyncDatabaseHelper.java
    ├── HapticFeedbackUtils.java
    ├── SnackbarHelper.java
    └── ValidationUtils.java
```

## CI/CD

| Trigger | Track | versionCode |
|---------|-------|-------------|
| Push no `master` | Internal (QA) | `100 + git rev-list --count HEAD` |
| Tag `v*` | Production | `100 + git rev-list --count HEAD` |

### Deploy para producao

```bash
git tag v2.x && git push origin v2.x
```

### Deploy para track interno (QA)

```bash
git push origin master
```

## Setup local

### Pre-requisitos

- Android Studio Iguana ou superior
- JDK 17+
- Arquivo `keystore.properties` na raiz (nao commitado, ver `keystore.properties.template`)

### Build

```bash
./gradlew assembleDebug        # debug
./gradlew bundleRelease        # release AAB (requer keystore.properties)
./gradlew :app:lintRelease     # lint
./gradlew :app:testReleaseUnitTest  # testes
```

### Instalar no dispositivo

```bash
./gradlew installDebug
```

## GitHub Secrets necessarios para CI

| Secret | Descricao |
|--------|-----------|
| `KEYSTORE_BASE64` | Keystore em base64 |
| `KEYSTORE_PASSWORD` | Senha do keystore |
| `KEY_ALIAS` | Alias da chave |
| `KEY_PASSWORD` | Senha da chave |
| `PLAY_STORE_SERVICE_ACCOUNT_JSON` | Service account JSON do Google Play |

## Seguranca

- HTTPS only (`usesCleartextTraffic="false"`)
- Queries parametrizadas em todos os DAOs (prevencao SQL injection)
- ProGuard/R8 com ofuscacao em release
- Keystore nunca commitado (`.gitignore` protege `*.keystore`, `keystore.properties`)
- GitHub Actions pinadas por SHA de commit

## Repositorio

**GitHub**: https://github.com/Vitorspk/amigosecreto
**Branch principal**: `master`
**Package Play Store**: `com.amigosecreto.sorteio`
