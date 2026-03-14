# AmigoSecreto — Documentação para Claude

## Visão Geral

**AmigoSecreto** é um aplicativo Android para organizar sorteios de amigo secreto. Permite criar grupos, adicionar participantes, realizar sorteios com restrições, revelar resultados de forma interativa, compartilhar via WhatsApp/SMS e gerenciar listas de desejos por participante.

| Campo | Valor |
|-------|-------|
| Versão atual | 2.0 (versionCode: `100 + git rev-list --count HEAD`, produção ~157+) |
| Application ID | `com.amigosecreto.sorteio` |
| Package Java | `activity.amigosecreto` |
| Min SDK | 21 (Android 5.0) |
| Target / Compile SDK | 35 (Android 15) |
| Linguagem | Java 17 |
| Branch principal | `master` |

---

## Regras de Trabalho

- **Nunca commitar diretamente na `master`.** Sempre criar branch → commitar → abrir PR.
- Tipos de branch: `fix/`, `feat/`, `chore/`, `docs/`
- Sempre rodar `./gradlew :app:testDebugUnitTest` antes de abrir PR.
- Sempre rodar `./gradlew :app:lintRelease` e não introduzir novos erros de lint.
- Cobertura mínima: 80% — novos métodos de lógica de negócio precisam de testes.

---

## Estrutura do Projeto

```
app/src/main/java/activity/amigosecreto/
├── GruposActivity.java                    # LAUNCHER — tela principal, gerenciar grupos
├── ParticipantesActivity.java             # gerenciar participantes de um grupo
├── ParticipantesViewModel.java            # MVVM ViewModel — lógica de negócio + LiveData
├── RevelarAmigoActivity.java              # revelar amigo secreto interativamente
├── ParticipanteDesejosActivity.java       # ver desejos de um participante
├── VisualizarDesejosActivity.java         # ver todos os desejos de um grupo
├── ListarDesejosActivity.java             # lista de desejos geral
├── InserirDesejoActivity.java             # adicionar novo desejo
├── AlterarDesejoActivity.java             # editar desejo existente
├── DetalheDesejoActivity.java             # detalhes do desejo + busca Buscape
│
├── adapter/
│   └── ParticipantesRecyclerAdapter.java  # RecyclerView adapter para participantes
│
├── db/
│   ├── MySQLiteOpenHelper.java            # schema SQLite v8 + migrações
│   ├── Grupo.java                         # model de grupo (Serializable)
│   ├── GrupoDAO.java                      # CRUD de grupos
│   ├── Participante.java                  # model de participante (Serializable)
│   ├── ParticipanteDAO.java               # CRUD + exclusões + sorteio + transações atômicas
│   ├── Desejo.java                        # model de desejo (Serializable)
│   └── DesejoDAO.java                     # CRUD de desejos + batch queries (N+1 eliminado)
│
├── repository/
│   ├── ParticipanteRepository.java        # encapsula ParticipanteDAO; síncrono, thread de BG
│   └── DesejoRepository.java             # encapsula DesejoDAO; síncrono, thread de BG
│
└── util/
    ├── AnimationUtils.java                # animações reutilizáveis
    ├── AsyncDatabaseHelper.java           # operações assíncronas no banco
    ├── HapticFeedbackUtils.java           # feedback háptico (flags=0, respeita acessibilidade)
    ├── MensagemSecretaBuilder.java        # formata mensagem de compartilhamento do sorteio
    ├── SnackbarHelper.java                # mensagens padronizadas
    ├── SorteioEngine.java                 # motor de sorteio (extraído para testabilidade)
    ├── ValidationUtils.java               # validação centralizada de inputs
    └── WindowInsetsUtils.java             # IME padding, Locale pt-BR, formatação monetária
```

```
.github/workflows/
├── release.yml          # deploy automático → Play Store production (tag v*)
├── ci.yml               # CI + deploy internal track (push master)
├── claude-code-review.yml  # review automático de PRs
├── pr-checks.yml        # validações de PR
└── claude.yml           # workflow Claude

distribution/whatsnew/
├── whatsnew-pt-BR       # release notes em português
└── whatsnew-en-US       # release notes em inglês

documents/
├── TECHNICAL_ANALYSIS.md   # análise técnica completa e roadmap
├── TEST_PLAN.md             # plano de testes (Fases 1–3)
└── RELEASE_INSTRUCTIONS.md # processo de release detalhado
```

---

## CI/CD Pipeline

### Deploy para Produção (tag)

```bash
git tag v2.x
git push origin v2.x
```

**Workflow:** `.github/workflows/release.yml`
- Trigger: push de tag `v*` (formato: `v2.1` ou `v2.1.0`)
- versionCode: `100 + git rev-list --count HEAD`
- versionName: extraído da tag (`v2.1` → `2.1`)
- Steps: checkout → JDK 21 → lint → testes → `bundleRelease` → Play Store (production) → GitHub Release
- Todas as actions pinadas por commit SHA

### CI / Deploy Interno (master)

```bash
git push origin master
```

**Workflow:** `.github/workflows/ci.yml`
- Trigger: push no master (excluindo tags `v*`)
- versionName: `2.0-dev.<short-sha>`
- Track: **internal** (QA antes de produção)
- `cancel-in-progress: true`

### Signing Config

O `build.gradle` suporta dois modos:
1. **Local:** lê de `keystore.properties` (arquivo não commitado)
2. **CI:** lê de environment variables (`CI_KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, etc.)

### GitHub Secrets Necessários

| Secret | Descrição |
|--------|-----------|
| `KEYSTORE_BASE64` | Keystore em base64 |
| `KEYSTORE_PASSWORD` | Senha do keystore |
| `KEY_ALIAS` | Alias da chave (ex: `amigosecreto`) |
| `KEY_PASSWORD` | Senha da chave |
| `PLAY_STORE_SERVICE_ACCOUNT_JSON` | JSON da service account do Google Play |

---

## Banco de Dados

### Nome: `amigosecreto_v8.db` (versão 8)

```sql
CREATE TABLE grupo (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome TEXT NOT NULL,
    data TEXT
);

CREATE TABLE participante (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome TEXT NOT NULL,
    email TEXT,
    telefone TEXT,
    amigo_sorteado_id INTEGER,
    enviado INTEGER DEFAULT 0,
    grupo_id INTEGER,
    FOREIGN KEY(grupo_id) REFERENCES grupo(id)
);

CREATE TABLE exclusao (
    participante_id INTEGER,
    excluido_id INTEGER,
    PRIMARY KEY (participante_id, excluido_id),
    FOREIGN KEY(participante_id) REFERENCES participante(id) ON DELETE CASCADE,
    FOREIGN KEY(excluido_id) REFERENCES participante(id) ON DELETE CASCADE
);

CREATE TABLE desejo (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    produto TEXT NOT NULL,
    categoria TEXT,
    preco_minimo REAL,
    preco_maximo REAL,
    lojas TEXT,
    participante_id INTEGER,
    FOREIGN KEY(participante_id) REFERENCES participante(id)
);
```

### Migrações

- `< v7` → drop e recria tudo (versões muito antigas)
- `v7 → v8` → adiciona coluna `participante_id` na tabela `desejo`

---

## Funcionalidades

### 1. Sistema de Grupos
- Criar/editar/remover grupos de amigo secreto
- Cada grupo tem seus próprios participantes e sorteio
- Entrada principal via `GruposActivity` (LAUNCHER)

### 2. Gerenciamento de Participantes
- Adicionar manualmente ou importar dos contatos
- Remover individual ou limpar todos do grupo
- RecyclerView com animações de entrada
- Mínimo de 3 participantes para sorteio

### 3. Exclusões (Restrições)
- Definir quem NÃO pode tirar quem
- Tabela `exclusao` com chave composta
- Validação durante o sorteio

### 4. Sorteio
- Algoritmo de embaralhamento com validação
- Ninguém tira a si mesmo
- Respeita exclusões definidas
- Resultados persistidos no banco
- Transação atômica via `salvarSorteio()`

### 5. Revelação Interativa
- Resultado acessível apenas via botão de compartilhamento individual por participante
- Organizador não consegue ver quem tirou quem diretamente na lista
- `RevelarAmigoActivity` disponível para uso futuro (o próprio participante revela no celular)
- Proteção contra spoilers (layout escondido)
- Animações e feedback háptico

### 6. Compartilhamento
- WhatsApp com proteção anti-spoiler (30 linhas em branco)
- SMS via intent nativo
- Marca como "enviado" após compartilhar
- URL encoding seguro via `Uri.Builder`
- **Limitação conhecida:** `marcarComoEnviado` é chamado antes do usuário confirmar o share sheet (a API `ACTION_SEND` não oferece callback de confirmação). Se o usuário abrir o chooser e cancelar, o participante ficará marcado como "enviado" sem que a mensagem tenha sido de fato enviada.

### 7. Lista de Desejos
- CRUD completo: produto, categoria, faixa de preço, lojas
- Vinculada a participante via `participante_id`
- Compartilhamento formatado
- Integração Buscape (HTTPS via `Uri.Builder`)

---

## Tecnologias e Dependências

### Build
- Android Gradle Plugin: 9.0.1
- Compile SDK: 35
- Java: 17
- ViewBinding: habilitado
- MultiDex: habilitado
- R8/ProGuard: minificação + shrink em release

### Dependências
```gradle
implementation 'androidx.appcompat:appcompat:1.7.0'
implementation 'com.google.android.material:material:1.12.0'
implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
implementation 'androidx.recyclerview:recyclerview:1.3.2'
implementation 'androidx.core:core-splashscreen:1.0.1'

testImplementation 'junit:junit:4.13.2'
testImplementation 'org.mockito:mockito-core:5.11.0'
testImplementation 'org.robolectric:robolectric:4.13'
testImplementation 'androidx.test:core:1.6.1'
testImplementation 'androidx.test.ext:junit:1.2.1'

androidTestImplementation 'androidx.test.ext:junit:1.2.1'
androidTestImplementation 'androidx.test.espresso:espresso-core:3.6.1'
androidTestImplementation 'androidx.test:runner:1.6.2'
androidTestImplementation 'androidx.test:rules:1.6.1'
```

### Permissões
- `INTERNET` — WhatsApp e Buscape
- `ACCESS_NETWORK_STATE` — verificação de conectividade
- `VIBRATE` — feedback háptico
- `READ_CONTACTS` — importar participantes

---

## Padrões de Arquitetura

### MVVM (ParticipantesActivity)
- `ParticipantesViewModel extends AndroidViewModel` com `LiveData` para todos os estados
- Todas as operações de banco executadas via `ExecutorService` (thread de background)
- Resultados postados de volta ao main thread via `Handler(Looper.getMainLooper())`
- Activity apenas observa LiveData e não toca diretamente nos repositórios
- `InstantTaskExecutorRule` + `syncExecutor` para testes determinísticos

### Repository Pattern (ParticipantesActivity)
- `ParticipanteRepository` / `DesejoRepository` desacoplam ViewModel dos DAOs
- Cada método abre e fecha o DAO via try/finally (sem estado aberto entre chamadas)
- Métodos síncronos — chamados sempre de thread de background
- `@VisibleForTesting(otherwise = PACKAGE_PRIVATE)` em métodos package-private expostos só para testes
- Erros de banco tratados via `handleDbError()` no ViewModel: loga com `Log.e` + posta `errorMessage` via `Handler.post` — nunca relança de dentro do executor (relançar de `Runnable` vai para `UncaughtExceptionHandler` sem feedback ao usuário)

### DAO Pattern
- `GrupoDAO`, `ParticipanteDAO`, `DesejoDAO`
- Queries parametrizadas (prevenção SQL injection)
- `getColumnIndexOrThrow()` para robustez na leitura de cursors
- Transações atômicas: `salvarSorteio()`, `salvarExclusoes()`
- Batch queries: `contarDesejosPorGrupo()` e `listarDesejosPorGrupo()` com INNER JOIN + GROUP BY (elimina N+1)

### Utility Layer
- `WindowInsetsUtils` — IME padding + locale `pt-BR` + formatação monetária (centralizado)
- `HapticFeedbackUtils` — `flags = 0` (respeita configurações de acessibilidade)
- `ValidationUtils` — validação centralizada de inputs
- `AsyncDatabaseHelper` — operações assíncronas
- `SnackbarHelper` — mensagens padronizadas
- `AnimationUtils` — animações reutilizáveis
- `SorteioEngine` — motor de sorteio extraído de `ParticipantesActivity` para testabilidade
- `MensagemSecretaBuilder` — formata mensagem de compartilhamento do sorteio

### Adapter Pattern
- `ParticipantesRecyclerAdapter` com `getBindingAdapterPosition()`
- Interface `OnItemClickListener` para item / remove / share

### Activity-based
- Cada tela = uma Activity
- Dados via Intent extras (Serializable)
- `GruposActivity` como ponto de entrada

---

## Fluxo do Usuário

```
[GruposActivity] — Tela Principal (LAUNCHER)
    ├── Criar / Editar / Remover Grupo
    └── Selecionar Grupo
            └── [ParticipantesActivity]
                    ├── Adicionar Participantes
                    │   ├── Manualmente (dialog)
                    │   └── Importar dos Contatos
                    ├── Configurar Exclusões
                    ├── Realizar Sorteio (>= 3 participantes)
                    ├── Editar Participante (nome, telefone, email)
                    ├── Compartilhar via WhatsApp/SMS
                    └── Ver Desejos
                            └── [VisualizarDesejosActivity]
                                        └── [ParticipanteDesejosActivity]

Menu Global
    └── [ListarDesejosActivity]
            ├── [InserirDesejoActivity]
            ├── [DetalheDesejoActivity]
            │   └── [AlterarDesejoActivity]
            └── Compartilhar Lista
```

---

## Esquema de Cores

**Paleta:** Indigo & Emerald

| Token | Hex | Uso |
|-------|-----|-----|
| `colorPrimary` | `#4F46E5` | Cor principal (Indigo) |
| `colorPrimaryDark` | `#3730A3` | Variante escura |
| `colorAccent` | `#10B981` | Destaque/sucesso (Emerald) |
| `background` | `#F9FAFB` | Fundo das telas |
| `text_primary` | `#111827` | Texto principal |
| `text_secondary` | `#6B7280` | Texto secundário |
| `error` | `#EF4444` | Ações destrutivas |
| `success` | `#10B981` | Feedback positivo |

---

## Comandos Úteis

### Build

```bash
./gradlew assembleDebug          # build debug
./gradlew installDebug           # instalar no dispositivo
./gradlew bundleRelease          # build release AAB (requer keystore.properties)
./gradlew clean                  # limpar build
./gradlew :app:lintRelease       # lint
./gradlew :app:testDebugUnitTest # testes unitários + Robolectric
```

### Testes

```bash
# Unitários e de DAO (sem emulador — rápido)
./gradlew :app:testDebugUnitTest

# Instrumentados (requer emulador/dispositivo)
./gradlew :app:connectedDebugAndroidTest

# Tudo junto
./gradlew :app:test :app:connectedCheck
```

### Deploy

```bash
# Produção (Play Store):
git tag v2.x && git push origin v2.x

# Interno (QA):
git push origin master
```

### Logs ADB

```bash
adb logcat | grep -i "amigosecreto"
adb logcat | grep -E "AndroidRuntime|FATAL"
```

### Banco de Dados (debug)

```bash
adb shell
run-as com.amigosecreto.sorteio.debug
cd databases && sqlite3 amigosecreto_v8.db
.tables
.schema participante
SELECT * FROM grupo;
SELECT * FROM participante WHERE grupo_id = 1;
```

---

## Edge-to-Edge (Android 15)

Todas as 9 Activities chamam `EdgeToEdge.enable(this)` antes de `setContentView()`.

| Tela | Estratégia |
|------|------------|
| CoordinatorLayout + AppBarLayout | `fitsSystemWindows="true"` — trata insets automaticamente |
| `RevelarAmigoActivity` | listener manual no `root_revelar` (LinearLayout sem toolbar) |
| `ParticipantesActivity` | listener no `layout_bottom_buttons` com `padBottom` capturado fora da lambda |
| `ListarDesejos` (FAB) | ajuste de `bottomMargin` via `requestLayout()` |
| `InserirDesejoActivity`, `AlterarDesejoActivity` | `WindowInsetsUtils.applyImeBottomPadding()` |

`android:statusBarColor` e `android:windowLightStatusBar` removidos do tema (deprecated no Android 15, conflitam com EdgeToEdge).

---

## Segurança

- **HTTPS only:** `usesCleartextTraffic="false"` no Manifest
- **Network Security Config:** `xml/network_security_config.xml`
- **ProGuard/R8:** ofuscação em release, remove logs de debug
- **Queries parametrizadas:** prevenção de SQL injection em todos os DAOs
- **FileProvider:** compartilhamento seguro de arquivos
- **Keystore nunca commitada:** signing via `keystore.properties` (local) ou env vars (CI)
- **Actions pinadas por SHA:** supply-chain security nos workflows
- **Backup rules:** `xml/backup_rules.xml` e `xml/data_extraction_rules.xml`

---

## Recursos de UI

### Animações (`res/anim/`)
`bounce.xml`, `button_press.xml`, `card_appear.xml`, `fade_in.xml`, `fade_out.xml`, `slide_in_left.xml`, `slide_in_right.xml`, `slide_out_left.xml`, `slide_out_right.xml`

### Layouts (`res/layout/`) — 21 arquivos
- 9 layouts de Activity
- 5 layouts de Dialog
- 7 layouts de Item/Helper (`empty_state.xml`, `loading_state.xml`, etc.)

### Drawables
- Gradientes, botões, backgrounds, ícones SVG, ripples
- Launcher icons com adaptive icon (API 26+)
- `ic_add.xml`, `ic_dice.xml`, `ic_arrow_forward.xml` — novos ícones tintáveis (`fillColor` preto, tint no call site)

---

## Testes

### Estrutura

```
app/src/test/java/activity/amigosecreto/
├── FormatarPrecoTest.java               # formatação de preço pt-BR
├── ParticipantesViewModelTest.java      # ViewModel — Robolectric + InstantTaskExecutorRule
├── db/
│   ├── DesejoModelTest.java             # model Desejo — Serializable
│   ├── GrupoDAOTest.java                # CRUD de grupos (Robolectric)
│   ├── GrupoModelTest.java              # model Grupo — Serializable
│   ├── MySQLiteOpenHelperTest.java      # schema do banco (Robolectric)
│   ├── ParticipanteDAOTest.java         # CRUD de participantes (Robolectric)
│   └── ParticipanteModelTest.java       # model Participante — Serializable
├── repository/
│   ├── DesejoRepositoryTest.java        # DesejoRepository — integração real SQLite
│   └── ParticipanteRepositoryTest.java  # ParticipanteRepository — integração real SQLite
└── util/
    ├── MensagemSecretaBuilderTest.java  # formatação de mensagem de compartilhamento
    ├── SorteioEngineTest.java           # algoritmo de sorteio — propriedades e exclusões
    └── ValidationUtilsTest.java         # validações de input e regex
```

### Cobertura Atual (224 testes — BUILD SUCCESSFUL)

| Camada | Arquivo | Casos |
|--------|---------|------:|
| Util | `MensagemSecretaBuilderTest` | 21 |
| Util | `ValidationUtilsTest` | 19 |
| Util | `SorteioEngineTest` | 11 |
| Util | `FormatarPrecoTest` | 12 |
| Model | `DesejoModelTest` | 18 |
| Model | `ParticipanteModelTest` | 11 |
| Model | `GrupoModelTest` | 7 |
| DAO | `ParticipanteDAOTest` | 21 |
| DAO | `DesejoDAOTest` | 20 |
| DAO | `GrupoDAOTest` | 12 |
| DAO | `MySQLiteOpenHelperTest` | 8 |
| Repository | `ParticipanteRepositoryTest` | 17 |
| Repository | `DesejoRepositoryTest` | 16 |
| ViewModel | `ParticipantesViewModelTest` | 31 |

**Configuração Robolectric:** `testOptions.unitTests.includeAndroidResources = true` habilitado em `build.gradle` para permitir acesso a recursos compilados (`getString()`) nos testes unitários.

Ver `documents/TEST_PLAN.md` para descrição detalhada das Fases 1–3 e progresso.

---

## Próximas Melhorias

Ver `documents/TECHNICAL_ANALYSIS.md` para análise completa e roadmap priorizado.

### Arquitetura
- [x] Extrair lógica de `ParticipantesActivity` para ViewModel/classes separadas (PR #18)
- [x] Migrar para MVVM com ViewModel e LiveData (PR #18)
- [x] Implementar Repository pattern (PR #19)
- [x] Testes de ViewModel com Robolectric + cobertura de caminhos de erro (PR #20)
- [ ] Adicionar Dependency Injection (Hilt)
- [ ] Migrar para Kotlin

### Qualidade
- [x] Mover ~150 strings hardcoded para `strings.xml` (PR #15 + PR #21)
- [x] Strings XML layouts/menus extraídas + acessibilidade corrigida (PR #21)
- [ ] Remover ~47 recursos não utilizados (Lint `UnusedResources`) — **próximo (PR #22)**
- [ ] Implementar `FOREIGN KEY ... ON DELETE CASCADE` na tabela `exclusao` no código Java (`MySQLiteOpenHelper`, schema v9)
- [ ] Testes de UI com Espresso
- [ ] Logs estruturados (Timber)

### UI/UX
- [ ] Modo escuro completo (suporte parcial em `values-night/`)
- [ ] Suporte a tablets
- [ ] Transições entre Activities com shared elements

### Funcionalidades
- [ ] Backup/restore de dados (Google Drive)
- [ ] Histórico de sorteios anteriores
- [ ] Notificações de lembrete
- [ ] Compartilhar via Telegram/Email
- [ ] QR Code para compartilhamento

---

## Repositório

**URL:** https://github.com/Vitorspk/amigosecreto
**Branch Principal:** `master`
**Package Play Store:** `com.amigosecreto.sorteio`
