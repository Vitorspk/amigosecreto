# AmigoSecreto - Documentacao do Projeto

## Visao Geral

**AmigoSecreto** e um aplicativo Android para organizar e gerenciar amigo secreto. O app permite criar grupos, adicionar participantes, realizar sorteios aleatorios com exclusoes, revelar resultados de forma interativa, compartilhar via WhatsApp/SMS e gerenciar listas de desejos por participante.

**Versao Atual**: 2.0 (versionCode: 9, CI usa `100 + commit count`)
**Package**: `activity.amigosecreto`
**Application ID**: `com.amigosecreto.sorteio`
**SDK Minimo**: 21 (Android 5.0)
**SDK Alvo/Compile**: 35 (Android 15)
**Java**: 17
**Branch Principal**: `master`

---

## Estrutura do Projeto

```
app/src/main/java/activity/amigosecreto/
├── GruposActivity.java                    # LAUNCHER - Tela principal, gerenciar grupos
├── ParticipantesActivity.java             # Gerenciar participantes de um grupo
├── RevelarAmigoActivity.java              # Revelar amigo secreto interativamente
├── ParticipanteDesejosActivity.java       # Ver desejos de um participante
├── VisualizarDesejosActivity.java         # Ver todos os desejos de um grupo
├── ListarDesejos.java                     # Listar desejos gerais
├── InserirDesejoActivity.java             # Adicionar novo desejo
├── AlterarDesejoActivity.java             # Editar desejo existente
├── DetalheDesejoActivity.java             # Detalhes do desejo
│
├── adapter/
│   └── ParticipantesRecyclerAdapter.java  # RecyclerView adapter para participantes
│
├── db/
│   ├── MySQLiteOpenHelper.java            # Schema do banco SQLite (v8)
│   ├── Grupo.java                         # Model de grupo (Serializable)
│   ├── GrupoDAO.java                      # CRUD de grupos
│   ├── Participante.java                  # Model de participante (Serializable)
│   ├── ParticipanteDAO.java               # CRUD de participantes + exclusoes + sorteio
│   ├── Desejo.java                        # Model de desejo
│   └── DesejoDAO.java                     # CRUD de desejos
│
└── util/
    ├── AnimationUtils.java                # Helpers de animacao
    ├── AsyncDatabaseHelper.java           # Operacoes async no banco
    ├── HapticFeedbackUtils.java           # Feedback haptico (respeita acessibilidade)
    ├── SnackbarHelper.java                # Helpers de Snackbar
    └── ValidationUtils.java              # Validacao de inputs
```

### Outros Diretorios Relevantes

```
.github/workflows/
├── release.yml                    # Deploy automatico para Play Store (tag v*)
├── ci.yml                         # CI + deploy internal track (push master)
├── claude-code-review.yml         # Review automatico de PRs
└── claude.yml                     # Workflow Claude

distribution/whatsnew/
├── whatsnew-pt-BR                 # Release notes em portugues
└── whatsnew-en-US                 # Release notes em ingles

documents/
├── PRIVACY_POLICY.md
├── PLAY_STORE_LISTING.md
├── PRODUCTION_CHECKLIST.md
├── WHATS_NEW_V2.md
└── RELEASE_INSTRUCTIONS.md
```

---

## CI/CD Pipeline

### Deploy para Producao (tag)

```bash
git tag v2.1
git push origin v2.1
```

**Workflow**: `.github/workflows/release.yml`
- Trigger: push de tag `v*` (formato: `v2.1` ou `v2.1.0`)
- versionCode: `100 + git rev-list --count HEAD`
- versionName: extraido da tag (v2.1 -> 2.1)
- Steps: checkout -> JDK 21 -> lint -> testes -> bundleRelease -> Play Store (production) -> GitHub Release
- Todas as actions pinadas por commit SHA

### CI / Deploy Interno (master)

```bash
git push origin master
```

**Workflow**: `.github/workflows/ci.yml`
- Trigger: push no master (excluindo tags v*)
- versionName: `2.0-dev.<short-sha>`
- Track: **internal** (QA antes de producao)
- `cancel-in-progress: true`

### GitHub Secrets Necessarios

| Secret | Descricao |
|--------|-----------|
| `KEYSTORE_BASE64` | Keystore em base64 |
| `KEYSTORE_PASSWORD` | Senha do keystore |
| `KEY_ALIAS` | Alias da chave (ex: amigosecreto) |
| `KEY_PASSWORD` | Senha da chave |
| `PLAY_STORE_SERVICE_ACCOUNT_JSON` | JSON da service account do Google Play |

### Signing Config

O `build.gradle` suporta dois modos:
1. **Local**: le de `keystore.properties` (arquivo nao commitado)
2. **CI**: le de environment variables (`CI_KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, etc.)

---

## Banco de Dados

### Nome: `amigosecreto_v8.db` (versao 8)

#### Tabela: `grupo`
```sql
CREATE TABLE grupo (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome TEXT NOT NULL,
    data TEXT
)
```

#### Tabela: `participante`
```sql
CREATE TABLE participante (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome TEXT NOT NULL,
    email TEXT,
    telefone TEXT,
    amigo_sorteado_id INTEGER,
    enviado INTEGER DEFAULT 0,
    grupo_id INTEGER,
    FOREIGN KEY(grupo_id) REFERENCES grupo(id)
)
```

#### Tabela: `exclusao`
```sql
CREATE TABLE exclusao (
    participante_id INTEGER,
    excluido_id INTEGER,
    PRIMARY KEY (participante_id, excluido_id)
)
```

#### Tabela: `desejo`
```sql
CREATE TABLE desejo (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    produto TEXT NOT NULL,
    categoria TEXT,
    preco_minimo REAL,
    preco_maximo REAL,
    lojas TEXT,
    participante_id INTEGER,
    FOREIGN KEY(participante_id) REFERENCES participante(id)
)
```

### Migracoes

- **< v7**: Drop e recria tudo (versoes muito antigas)
- **v7 -> v8**: Adiciona coluna `participante_id` na tabela `desejo`

---

## Funcionalidades

### 1. Sistema de Grupos
- Criar/editar/remover grupos de amigo secreto
- Cada grupo tem seus proprios participantes e sorteio
- Entrada principal via `GruposActivity` (LAUNCHER)

### 2. Gerenciamento de Participantes
- Adicionar manualmente ou importar dos contatos
- Remover individual ou limpar todos do grupo
- RecyclerView com animacoes de entrada
- Minimo de 3 participantes para sorteio

### 3. Exclusoes (Restricoes)
- Definir quem NAO pode tirar quem
- Tabela `exclusao` com chave composta
- Validacao durante o sorteio

### 4. Sorteio
- Algoritmo de embaralhamento com validacao
- Ninguem tira a si mesmo
- Respeita exclusoes definidas
- Resultados persistidos no banco
- Transacao atomica via `salvarSorteio()`

### 5. Revelacao Interativa
- Resultado acessivel apenas via botao de compartilhamento individual por participante
- Organizador nao consegue ver quem tirou quem diretamente na lista
- RevelarAmigoActivity disponivel para uso futuro (ex: o proprio participante revela no celular)
- Protecao contra spoilers (layout escondido na RevelarAmigoActivity)
- Animacoes e feedback haptico

### 6. Compartilhamento
- WhatsApp com protecao anti-spoiler (30 linhas em branco)
- SMS via intent nativo
- Marca como "enviado" apos compartilhar
- URL encoding seguro via `Uri.Builder`
- **Limitacao conhecida**: `marcarComoEnviado` e chamado antes do usuario confirmar o share sheet (a API `ACTION_SEND` nao oferece callback de confirmacao). Se o usuario abrir o chooser e cancelar, o participante ficara marcado como "enviado" sem que a mensagem tenha sido de fato enviada.

### 7. Lista de Desejos
- CRUD completo: produto, categoria, faixa de preco, lojas
- Vinculada a participante via `participante_id`
- Compartilhamento formatado
- Integracao BuscaPe (HTTPS via Uri.Builder)

---

## Tecnologias

### Build
- **Android Gradle Plugin**: 9.0.1
- **Compile SDK**: 35
- **Java**: 17
- **ViewBinding**: Habilitado
- **MultiDex**: Habilitado
- **R8/ProGuard**: Minificacao + shrink em release

### Dependencias
```gradle
implementation 'androidx.appcompat:appcompat:1.7.0'
implementation 'com.google.android.material:material:1.12.0'
implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
implementation 'androidx.recyclerview:recyclerview:1.3.2'
implementation 'androidx.core:core-splashscreen:1.0.1'
```

### Permissoes
- `INTERNET` - WhatsApp e BuscaPe
- `ACCESS_NETWORK_STATE` - Verificacao de conectividade
- `VIBRATE` - Feedback haptico
- `READ_CONTACTS` - Importar participantes

---

## Padroes de Arquitetura

### DAO Pattern
- `GrupoDAO`, `ParticipanteDAO`, `DesejoDAO`
- Queries parametrizadas (prevencao SQL injection)
- `getColumnIndexOrThrow()` para robustez na leitura de cursors
- Transacoes atomicas para sorteio

### Utility Layer
- `HapticFeedbackUtils` - flags = 0 (respeita configuracoes de acessibilidade)
- `ValidationUtils` - Validacao centralizada de inputs
- `AsyncDatabaseHelper` - Operacoes assincronas
- `SnackbarHelper` - Mensagens padronizadas
- `AnimationUtils` - Animacoes reutilizaveis

### Adapter Pattern
- `ParticipantesRecyclerAdapter` com `getBindingAdapterPosition()`
- Interface `OnItemClickListener` para item/remove/share

### Activity-based
- Cada tela = uma Activity
- Dados via Intent extras (Serializable)
- GruposActivity como ponto de entrada

---

## Fluxo do Usuario

```
[GruposActivity] - Tela Principal (LAUNCHER)
    ├── Criar Grupo
    ├── Selecionar Grupo
    │   └── [ParticipantesActivity]
    │       ├── Adicionar Participantes
    │       │   ├── Manualmente (dialog)
    │       │   └── Importar dos Contatos
    │       ├── Configurar Exclusoes
    │       ├── Realizar Sorteio (>= 3 participantes)
    │       ├── Editar Participante (nome, telefone, email)
    │       ├── Compartilhar via WhatsApp/SMS (resultado individual, inclui lista de desejos)
    │       └── Ver Desejos
    │           └── [VisualizarDesejosActivity]
    │               └── [ParticipanteDesejosActivity]
    └── Menu: Lista de Desejos
        └── [ListarDesejos]
            ├── [InserirDesejoActivity]
            ├── [DetalheDesejoActivity]
            │   └── [AlterarDesejoActivity]
            └── Compartilhar Lista
```

---

## Esquema de Cores

**Paleta**: Indigo & Emerald

| Nome | Hex | Uso |
|------|-----|-----|
| colorPrimary | #4F46E5 | Cor primaria (Indigo) |
| colorPrimaryDark | #3730A3 | Variante escura |
| colorAccent | #10B981 | Destaque/sucesso (Emerald) |
| background | #F9FAFB | Fundo de telas |
| text_primary | #111827 | Texto principal |
| text_secondary | #6B7280 | Texto secundario |
| error | #EF4444 | Acoes destrutivas |
| success | #10B981 | Feedback positivo |

---

## Comandos Uteis

### Build Local
```bash
./gradlew assembleDebug          # Build debug
./gradlew installDebug           # Instalar no dispositivo
./gradlew bundleRelease          # Build release AAB (requer keystore.properties)
./gradlew clean                  # Limpar build
./gradlew :app:lintRelease       # Rodar lint
./gradlew :app:testReleaseUnitTest  # Rodar testes
```

### Deploy via CI/CD
```bash
# Producao (Play Store):
git tag v2.1 && git push origin v2.1

# Interno (QA):
git push origin master
```

### Logs
```bash
adb logcat | grep -i "amigosecreto"
adb logcat | grep -E "AndroidRuntime|FATAL"
```

### Banco de Dados (Debug)
```bash
adb shell
run-as com.amigosecreto.sorteio.debug   # Nota: sufixo .debug em builds debug
cd databases
sqlite3 amigosecreto_v8.db

.tables
.schema participante
SELECT * FROM grupo;
SELECT * FROM participante WHERE grupo_id = 1;
```

---

## Recursos de UI

### Animacoes (res/anim/)
- `bounce.xml`, `button_press.xml`, `card_appear.xml`
- `fade_in.xml`, `fade_out.xml`
- `slide_in_left.xml`, `slide_in_right.xml`, `slide_out_left.xml`, `slide_out_right.xml`

### Layouts (res/layout/) - 21 arquivos
- 9 layouts de Activity
- 5 layouts de Dialog
- 7 layouts de Item/Helper (incluindo `empty_state.xml`, `loading_state.xml`)

### Drawables - 44 recursos XML
- Gradientes, botoes, backgrounds, icones SVG, ripples
- Launcher icons com adaptive icon (API 26+)

---

## Seguranca

- **HTTPS only**: `usesCleartextTraffic="false"` no Manifest
- **Network Security Config**: `xml/network_security_config.xml`
- **ProGuard/R8**: Ofuscacao em release, remove logs de debug
- **Queries parametrizadas**: Prevencao de SQL injection em todos os DAOs
- **FileProvider**: Compartilhamento seguro de arquivos
- **Keystore nunca commitado**: Signing via `keystore.properties` (local) ou env vars (CI)
- **Actions pinadas por SHA**: Supply-chain security nos workflows
- **Backup rules**: `xml/backup_rules.xml` e `xml/data_extraction_rules.xml`

---

## Proximas Melhorias

### Arquitetura
- [ ] Migrar para MVVM com ViewModel e LiveData
- [ ] Implementar Repository pattern
- [ ] Adicionar Dependency Injection (Hilt)
- [ ] Migrar para Kotlin

### UI/UX
- [ ] Modo escuro completo (Dark Theme) - suporte parcial em `values-night/`
- [ ] Suporte a tablets (layout responsivo)
- [ ] Transicoes entre Activities com shared elements

### Funcionalidades
- [ ] Backup/restore de dados (Google Drive)
- [ ] Historico de sorteios anteriores
- [ ] Notificacoes de lembrete
- [ ] Compartilhar via Telegram/Email
- [ ] QR Code para compartilhamento

### Qualidade
- [ ] Testes unitarios (JUnit) - cobertura atual minima
- [ ] Testes de UI (Espresso)
- [ ] Testes E2E
- [ ] Logs estruturados (Timber)

---

## Repositorio

**URL**: https://github.com/Vitorspk/amigosecreto
**Branch Principal**: `master`
**Package Play Store**: `com.amigosecreto.sorteio`
