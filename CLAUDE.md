# AmigoSecreto — Documentação para Claude

## Visão Geral

**AmigoSecreto** é um aplicativo Android para organizar sorteios de amigo secreto. Permite criar grupos, adicionar participantes, realizar sorteios com restrições, revelar resultados de forma interativa, compartilhar via WhatsApp/SMS e gerenciar listas de desejos por participante.

| Campo | Valor |
|-------|-------|
| Versão atual | 3.0 (versionCode: `100 + git rev-list --count HEAD`, produção ~157+) |
| Application ID | `com.amigosecreto.sorteio` |
| Package Java | `activity.amigosecreto` |
| Min SDK | 21 (Android 5.0) |
| Target / Compile SDK | 35 (Android 15) |
| Linguagem | Kotlin (migração completa — Fase 10f, PR #43) |
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
├── AmigoSecretoApplication.kt             # @HiltAndroidApp — ponto de entrada do Hilt
├── GruposActivity.kt                      # LAUNCHER — tela principal, gerenciar grupos
├── ParticipantesActivity.kt               # @AndroidEntryPoint — gerenciar participantes de um grupo
├── ParticipantesViewModel.kt              # @HiltViewModel — lógica de negócio + LiveData
├── RevelarAmigoActivity.kt               # revelar amigo secreto interativamente
├── ParticipanteDesejosActivity.kt         # ver desejos de um participante
├── VisualizarDesejosActivity.kt           # ver todos os desejos de um grupo
├── ListarDesejos.kt                       # lista de desejos geral
├── InserirDesejoActivity.kt               # adicionar novo desejo
├── AlterarDesejoActivity.kt               # editar desejo existente
├── DetalheDesejoActivity.kt               # detalhes do desejo + busca Buscape
│
├── adapter/
│   └── ParticipantesRecyclerAdapter.kt    # RecyclerView adapter para participantes
│
├── db/
│   ├── MySQLiteOpenHelper.kt              # schema SQLite v9 + migrações (ON DELETE CASCADE — PR #47)
│   ├── Grupo.kt                           # model de grupo (Serializable) — Kotlin
│   ├── GrupoDAO.kt                        # CRUD de grupos — Kotlin
│   ├── Participante.kt                    # model de participante (Serializable) — Kotlin
│   ├── ParticipanteDAO.kt                 # CRUD + exclusões + sorteio + transações atômicas — Kotlin
│   ├── Desejo.kt                          # model de desejo (Parcelable via @Parcelize) — Kotlin
│   └── DesejoDAO.kt                       # CRUD de desejos + batch queries (N+1 eliminado) — Kotlin
│
├── di/
│   └── DatabaseModule.kt                  # @Module @InstallIn(SingletonComponent) — provê Repositories
│
├── repository/
│   ├── ParticipanteRepository.kt          # encapsula ParticipanteDAO; chamado de coroutine IO — Kotlin
│   └── DesejoRepository.kt               # encapsula DesejoDAO; chamado de coroutine IO — Kotlin
│
└── util/
    ├── AnimationUtils.kt                  # animações reutilizáveis — Kotlin
    ├── AsyncDatabaseHelper.kt             # operações assíncronas no banco — Kotlin
    ├── HapticFeedbackUtils.kt             # feedback háptico (flags=0, respeita acessibilidade) — Kotlin
    ├── MensagemSecretaBuilder.kt          # formata mensagem de compartilhamento do sorteio — Kotlin
    ├── SnackbarHelper.kt                  # mensagens padronizadas — Kotlin
    ├── SorteioEngine.kt                   # motor de sorteio (extraído para testabilidade) — Kotlin
    ├── StateViewHelper.kt                 # gerencia estados loading/empty/content via ViewStub — Kotlin (PR #49)
    ├── ValidationUtils.kt                 # validação centralizada de inputs — Kotlin
    └── WindowInsetsUtils.kt               # IME padding, Locale pt-BR, formatação monetária — Kotlin
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
git tag v3.x
git push origin v3.x
```

**Workflow:** `.github/workflows/release.yml`
- Trigger: push de tag `v*` (formato: `v3.0` ou `v3.0.0`)
- versionCode: `100 + git rev-list --count HEAD`
- versionName: extraído da tag (`v3.0` → `3.0`)
- Steps: checkout → JDK 21 → lint → testes → `bundleRelease` → Play Store (production) → GitHub Release
- Todas as actions pinadas por commit SHA

### CI / Deploy Interno (master)

```bash
git push origin master
```

**Workflow:** `.github/workflows/ci.yml`
- Trigger: push no master (excluindo tags `v*`)
- versionName: `3.0-dev.<short-sha>`
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
implementation 'com.google.dagger:hilt-android:2.51.1'
kapt 'com.google.dagger:hilt-compiler:2.51.1'

implementation 'androidx.appcompat:appcompat:1.7.0'
implementation 'com.google.android.material:material:1.12.0'
implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
implementation 'androidx.recyclerview:recyclerview:1.3.2'

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

### Dependency Injection (Hilt)
- `@HiltAndroidApp` em `AmigoSecretoApplication` — ponto de geração de componentes
- `@HiltViewModel` + `@Inject` em `ParticipantesViewModel` — recebe Repositories via DI
- `@AndroidEntryPoint` em `ParticipantesActivity` — habilita injeção na Activity
- `DatabaseModule` (`@Module @InstallIn(SingletonComponent)`) — provê `ParticipanteRepository` e `DesejoRepository` como singletons
- Testes de ViewModel instanciam o ViewModel diretamente via construtor (sem Hilt no unit test)

### MVVM (ParticipantesActivity)
- `ParticipantesViewModel : AndroidViewModel` com `LiveData` para todos os estados — Kotlin (PR #39)
- Operações de banco executadas via `viewModelScope.launch { withContext(Dispatchers.IO) }` (PR #48)
- Activity apenas observa LiveData e não toca diretamente nos repositórios
- `InstantTaskExecutorRule` + `UnconfinedTestDispatcher` para testes determinísticos (PR #43 + PR #50)
- `private var` repositories — requerido por `@VisibleForTesting setRepositories()` — TODO: converter para `val` com constructor injection ou `@TestInstallIn`
- `@Volatile grupoId` — escrito no main thread, lido em coroutines de IO

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
git tag v3.x && git push origin v3.x

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
`card_appear.xml`, `fade_in.xml`, `fade_out.xml`, `slide_in_left.xml`, `slide_in_right.xml`, `slide_out_left.xml`, `slide_out_right.xml`

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

Todos os 19 arquivos de teste unitário estão em Kotlin. Testes instrumentados (Espresso) em `androidTest/`.

```
app/src/test/java/activity/amigosecreto/
├── FormatarPrecoTest.kt               # formatação de preço pt-BR
├── ParticipantesViewModelTest.kt      # ViewModel — Robolectric + InstantTaskExecutorRule
├── db/
│   ├── DesejoDAOBatchQueryTest.kt     # contarDesejosPorGrupo / listarDesejosPorGrupo (Robolectric)
│   ├── DesejoDAOTest.kt               # CRUD de desejos (Robolectric)
│   ├── DesejoModelTest.kt             # model Desejo — Serializable
│   ├── DesejoParcelableTest.kt        # Parcelable round-trip — @Parcelize via reflexão
│   ├── GrupoDAOTest.kt                # CRUD de grupos (Robolectric)
│   ├── GrupoModelTest.kt              # model Grupo — Serializable
│   ├── MySQLiteOpenHelperTest.kt      # schema do banco (Robolectric)
│   ├── ParticipanteDAOTest.kt         # CRUD de participantes (Robolectric)
│   ├── ParticipanteKotlinMigrationTest.kt  # contratos equals/hashCode/mutabilidade
│   └── ParticipanteModelTest.kt       # model Participante — Serializable
├── repository/
│   ├── DesejoRepositoryTest.kt        # DesejoRepository — integração real SQLite
│   ├── ParticipanteRepositoryTest.kt  # ParticipanteRepository — integração real SQLite
│   └── ParticipanteRepositorySalvarExclusoesTest.kt  # salvarExclusoes() transação atômica
└── util/
    ├── MensagemSecretaBuilderTest.kt  # formatação de mensagem de compartilhamento
    ├── SorteioEngineTest.kt           # algoritmo de sorteio — propriedades e exclusões
    ├── StateViewHelperTest.kt         # estados de UI: loading/empty/content (PR #49)
    └── ValidationUtilsTest.kt         # validações de input e regex

app/src/androidTest/java/activity/amigosecreto/
└── ParticipantesActivityTest.kt       # Espresso — fluxos críticos de ParticipantesActivity (PR #51)
```

### Cobertura Atual (297 testes unitários — BUILD SUCCESSFUL)

| Camada | Arquivo | Casos |
|--------|---------|------:|
| Util | `MensagemSecretaBuilderTest` | 24 |
| Util | `ValidationUtilsTest` | 36 |
| Util | `SorteioEngineTest` | 11 |
| Util | `StateViewHelperTest` | 12 |
| Util | `FormatarPrecoTest` | 12 |
| Model | `DesejoModelTest` | 18 |
| Model | `ParticipanteModelTest` | 11 |
| Model | `GrupoModelTest` | 7 |
| Model | `DesejoParcelableTest` | 6 |
| Model | `ParticipanteKotlinMigrationTest` | 12 |
| DAO | `ParticipanteDAOTest` | 23 |
| DAO | `DesejoDAOTest` | 20 |
| DAO | `GrupoDAOTest` | 12 |
| DAO | `MySQLiteOpenHelperTest` | 8 |
| DAO | `DesejoDAOBatchQueryTest` | 11 |
| Repository | `ParticipanteRepositoryTest` | 17 |
| Repository | `DesejoRepositoryTest` | 16 |
| Repository | `ParticipanteRepositorySalvarExclusoesTest` | 7 |
| ViewModel | `ParticipantesViewModelTest` | 33 |

**Espresso (androidTest):** `ParticipantesActivityTest` — testes instrumentados (PR #51)

**Configuração Robolectric:** `testOptions.unitTests.includeAndroidResources = true` habilitado em `build.gradle` para permitir acesso a recursos compilados (`getString()`) nos testes unitários.

Ver `documents/TEST_PLAN.md` para descrição detalhada das Fases 1–3 e progresso.

---

## Model Layer — Decisões de Design (PR #33)

### Por que plain class e não data class?

`Participante` e `Grupo` são plain classes; `Desejo` também é plain class com `equals`/`hashCode` explícitos.
`data class` gera `equals`/`hashCode` baseados em campos — com `var` fields mutáveis, objetos inseridos em coleções ficam inencontráveis após mutação. Plain class usa referência por padrão (ou override explícito de id).

### equals/hashCode por id (Desejo)

`Desejo.equals` e `hashCode` usam apenas `id` (chave primária do banco). Mudança semântica em relação ao Java (que comparava todos os campos). Auditado: nenhum call site de produção usa `contains`/`remove`/`Set`/`Map` com `Desejo` — todos os usos são `List<Desejo>` iterada por índice.

### var fields e DAOs

Os três models usam `var` porque os DAOs populam instâncias via property assignment após construção.
DAOs já migraram para Kotlin (Fase 10c — PR #38). Fields podem virar `val` com construtor primário quando/se migrar para Room.

### serialVersionUID em companion object

`private const val serialVersionUID: Long = 1L` em `companion object` compila para `ConstantValue: long 1l` no outer class (verificado via `javap`). Java serialization lê corretamente — não é necessário `@JvmStatic` ou `@JvmField`.

### Construtores de Desejo

`Desejo` tem construtor primário (no-arg, usado pelos DAOs) e um secundário `Desejo(id, produto)` que preserva o call site Java nos testes. `@JvmOverloads` foi removido — gerava 8 overloads, dos quais 6 eram inutilizados.

### toString() null → ""

`Grupo.toString()` e `Participante.toString()` retornam `""` para `nome` nulo (Kotlin `String` não aceita `null`). Java retornava `null`. Auditado: GruposActivity usa `getNome()` diretamente, não `toString()` implícito — sem regressão de UI.

### codigoAcesso (campo órfão)

`Participante.codigoAcesso` existe no model mas não tem coluna `codigo_acesso` no banco e não é lido/gravado por nenhum DAO. Campo mantido no model por compatibilidade — candidato a remoção em cleanup futuro.

---

## DAO + Repository Layer — Decisões de Design (PR #38)

### open class + open fun em ParticipanteRepository

`ParticipanteRepository` é `open class` com todos os métodos `open` porque os testes Kotlin (`ParticipantesViewModelTest.kt`) criam subclasses anônimas para injetar comportamento. Kotlin classes são `final` por padrão — sem `open`, a compilação falha com "cannot inherit from final". TODO pendente: converter para `internal` + `@VisibleForTesting` e remover `open` quando migrar injeção de fakes para constructor injection ou `@TestInstallIn`.

### Construtor triplo em Repositories

`private constructor(dao)` — construção interna; `constructor(context)` — produção via Hilt; `internal constructor(dao, forTesting: Boolean)` — injeção de DAO nos testes. O parâmetro `forTesting: Boolean` existe apenas para diferenciar a assinatura do construtor primário.

### listarPorGrupo — 2 passes para eliminar N+1

Pass 1 busca participantes em `LinkedHashMap` (preserva `ORDER BY nome`). Pass 2 busca todas as exclusões do grupo em uma única query `WHERE participante_id IN ($ids)`. IDs são inteiros vindos do próprio banco — interpolação sem risco de SQL injection. `rawQuery` com `?` não suporta listas dinâmicas no IN.

### NOME_AMIGO_DESCONHECIDO em companion object

`ParticipanteDAO.NOME_AMIGO_DESCONHECIDO = "Ninguém"` extraído para constante pública porque `RevelarAmigoActivity` precisa comparar com o fallback. TODO pendente: mover para `strings.xml` e fazer `getNomeAmigoSorteado()` retornar `String?`.

### proximoId() removido (PR #46)

`DesejoDAO.proximoId()` e `DesejoRepository.proximoId()` foram removidos (PR #46). `InserirDesejoActivity` foi refatorada para usar `repo.inserir(desejo)` diretamente via `AsyncDatabaseHelper`.

### mapearDesejosCursor — helper privado

`DesejoDAO.mapearDesejosCursor(cursor: Cursor)` elimina duplicação entre `listar()` e `listarPorParticipante()`. Usa `getColumnIndexOrThrow()` em todos os campos — falha ruidosamente em schema mismatch.

### Ordem de deleção — ON DELETE CASCADE (schema v9, PR #47)

Schema v9 adicionou `ON DELETE CASCADE` nas FKs de `exclusao` e `desejo`. A deleção em cascade é gerenciada pelo SQLite — `GrupoDAO.remover()` e `ParticipanteDAO.remover()` não precisam mais deletar manualmente filhos antes do pai.

---

## ViewModel Layer — Decisões de Design (PR #39)

### Java interop removido — result classes (PR #43)

`@JvmField` removido de `SorteioResultado`, `MensagensSmsResultado` e `MensagemCompartilhamentoResultado` — não há mais callers Java. Campos acessados diretamente via property Kotlin (`resultado.participante`, `resultado.mensagem`).

### @get:JvmName removido (PR #43)

`@get:JvmName("getIsLoading")` removido — `ParticipantesActivity` e os testes estão em Kotlin; acesso via property `isLoading` funciona diretamente.

### private var repositories

`participanteRepository` e `desejoRepository` são `var` (não `val`) porque `setRepositories()` (`@VisibleForTesting`) ainda é usado nos testes para injetar fakes. TODO pendente: converter para `val`, remover `setRepositories()` e migrar para constructor injection ou `@TestInstallIn` do Hilt.

### Coroutines — viewModelScope + Dispatchers.IO (PR #48)

A Fase 10d manteve `ExecutorService` + `Handler.post`; migração para coroutines foi feita no PR #48. O ViewModel agora usa `viewModelScope.launch { withContext(Dispatchers.IO) { ... } }` para todas as operações de banco. Testes usam `UnconfinedTestDispatcher` injetado via `ioDispatcher` (PR #50 adicionou `Dispatchers.setMain/resetMain` no setUp/tearDown).

### handleDbError() — padrão de tratamento de erros

Todos os catch blocks chamam `handleDbError(e, logMsg, errorStringRes)` que: (1) loga via `Log.e` com stack trace, (2) posta `errorMessage` no main thread. Exceções nunca são relançadas de dentro do executor — relançar de `Runnable` vai para `UncaughtExceptionHandler` sem feedback ao usuário.

### realizarSorteio — inline try/catch

`realizarSorteio` usa try/catch inline (não `handleDbError`) porque o resultado conduz um bloco `postMain` tri-state: sucesso recarrega + posta SUCCESS, falha posta `error_save_draw`. `handleDbError` postaria `errorMessage` diretamente via `Handler.post`, criando race condition com o bloco `postMain` e deixando `_isLoading` preso em `true`.

### takeIf { it > 0 } para amigoSorteadoId

`p.amigoSorteadoId?.takeIf { it > 0 }` substitui o duplo guard Java (`!= null && > 0`) em `prepararMensagensSms` e `prepararMensagemCompartilhamento`. `amigoSorteadoId = 0` indica "não sorteado" no schema — sem o guard, `nomeMap[0]` ou `desejosMap[0]` retornariam dados do participante errado.

### Ordem em prepararMensagemCompartilhamento

`MensagemSecretaBuilder.gerar()` é chamado **antes** de `marcarComoEnviado()`. Se `gerar()` lançar exceção, o participante não é marcado. Inverter a ordem causaria marcação sem mensagem entregue (bug de dados silencioso). Ver KDoc do método.

---

## Utility Layer — Decisões de Design (PR #36)

### trim() em produto/categoria/lojas (MensagemSecretaBuilder)

No Java original, `trim()` era usado apenas na validação (`isEmpty()` após `trim()`), mas o valor bruto era appendado. Em Kotlin, o valor trimado é appendado diretamente. Comportamento intencionalmente melhorado: espaços acidentais não aparecem na mensagem final. Documentado com três testes em `MensagemSecretaBuilderTest`.

### tentarSorteio(list, random) — visibilidade internal (PR #43)

O overload com `Random` era `public` (para ser acessível de `SorteioEngineTest.java`); agora é `internal @VisibleForTesting` — `SorteioEngineTest.kt` (Kotlin) acessa diretamente. Encapsulamento restabelecido. O overload público `tentarSorteio(list)` mantém `@JvmStatic` e continua `public` — não era shim e não foi alterado.

### kotlin.random.Random em SorteioEngine

Migrado de `java.util.Random` para `kotlin.random.Random` (PR #53). `Random(seed)` funciona identicamente para testes com seed fixa. A API `random.nextInt(n)` é a mesma.

### formatarPreco / numberFormatPtBr (MensagemSecretaBuilder → WindowInsetsUtils)

`MensagemSecretaBuilder.formatarPreco()` delega para `WindowInsetsUtils.numberFormatPtBr()`. Esse método de formatação pt-BR pertence logicamente a um `FormatUtils` separado, não a `WindowInsetsUtils`. **TODO:** extrair para `FormatUtils.kt` na Fase 10b restante ou 10c, junto com `LOCALE_PT_BR`.

---

## StateViewHelper — Decisões de Design (PR #49)

### Por que ViewStub?

`ViewStub` adia a inflação dos layouts de loading e empty até o primeiro uso. Telas que carregam rápido nunca pagam o custo de inflar esses layouts. A inflação é feita uma única vez — chamadas subsequentes ao mesmo estado apenas alteram `visibility`.

### Observer ordering — isLoading vs participants

O ViewModel posta `_participants` via `postValue()` **antes** de `_isLoading = false`. Por isso, o observer de `isLoading` em `ParticipantesActivity` drive apenas o estado loading:

```kotlin
// CORRETO — isLoading controla só o loading state
viewModel.isLoading.observe(this) { loading ->
    if (loading) stateHelper.showLoading()
}

// ERRADO — else causaria race condition: lista está vazia no momento
// em que isLoading=false dispara, antes do observers de participants atualizar
viewModel.isLoading.observe(this) { loading ->
    if (loading) stateHelper.showLoading()
    else if (participants.isEmpty()) stateHelper.showEmpty() // RACE CONDITION
    else stateHelper.showContent()
}
```

As transições empty/content são controladas exclusivamente pelo observer de `participants`, que já recebeu os dados quando dispara.

### layout_height="match_parent" nos ViewStubs

Dentro de `LinearLayout`, `ViewStub` com `layout_height="wrap_content"` herdaria essa altura para o filho inflado — loading spinners e empty states precisam de `match_parent` para centralizar corretamente. O `LayoutParams` do filho inflado herda do `ViewStub`.

### Testes com Mockito (não ViewStub real)

`ViewStub.inflate()` requer que o stub esteja attachado a uma janela. Em Robolectric, `ViewStub` real lança `IllegalStateException` se o parent não está no layout tree. A solução é mockar o `ViewStub` e retornar uma `View` real criada via `RuntimeEnvironment.getApplication()`:

```kotlin
stubLoading = mock(ViewStub::class.java)
`when`(stubLoading.inflate()).thenReturn(loadingView)
```

### AsyncDatabaseHelper em GruposActivity e ListarDesejos

`showLoading()` é chamado antes de `AsyncDatabaseHelper.execute()`. Sem async, `showLoading()` era imediatamente sobrescrito pelo resultado síncrono — o spinner nunca renderizava (Choreographer não tinha chance de desenhar). Com async, loading é visível enquanto o banco responde em background.

---

## Próximas Melhorias

Ver `documents/TECHNICAL_ANALYSIS.md` para análise completa e roadmap priorizado.

### Roadmap Priorizado

| # | Tarefa | Status | PR |
|---|--------|--------|----|
| 1 | **Cleanup pós-migração** — remover `proximoId()` + refatorar `InserirDesejoActivity` | ✅ Concluído | #46 |
| 2 | **Schema v9** — `ON DELETE CASCADE` nas FKs de `exclusao` e `desejo` | ✅ Concluído | #47 |
| 3 | **Coroutines** — `viewModelScope` + `Dispatchers.IO` no ViewModel | ✅ Concluído | #48 |
| 4 | **Estados de UI** — loading/empty/content via `StateViewHelper` + `ViewStub` | ✅ Concluído | #49 |
| 5 | **Testes Espresso** — `ParticipantesActivity` | ✅ Concluído | #51 |
| 6 | **kotlin.random.Random** — substituir `java.util.Random` em `SorteioEngine` | ✅ Concluído | #53 |
| — | **Testes de UI adicionais** — Espresso (GruposActivity, mais casos) | ⏳ Próximo | — |
| — | **Funcionalidades novas** — backup/restore, histórico de sorteios, etc. | ⏳ | — |

### Arquitetura (Concluído)
- [x] Extrair lógica de `ParticipantesActivity` para ViewModel/classes separadas (PR #18)
- [x] Migrar para MVVM com ViewModel e LiveData (PR #18)
- [x] Implementar Repository pattern (PR #19)
- [x] Testes de ViewModel com Robolectric + cobertura de caminhos de erro (PR #20)
- [x] Adicionar Dependency Injection (Hilt) — PR #29
- [x] Migrar models para Kotlin — Fase 10a (PR #33)
- [x] Migrar todos os utilitários (`util/`) para Kotlin — Fase 10b completa (PR #36 + PR #37)
- [x] Migrar DAOs e Repositories para Kotlin — Fase 10c (PR #38)
- [x] Migrar ViewModel para Kotlin — Fase 10d (PR #39)
- [x] Migrar Activities — Fase 10e (PR #41)
- [x] Migrar testes Java para Kotlin e remover shims de interop — Fase 10f (PR #43)
- [x] Estados de UI loading/empty/content via `StateViewHelper` + `ViewStub` (PR #49)
- [x] Testes Espresso para `ParticipantesActivity` (PR #51)

### Qualidade
- [x] Mover ~150 strings hardcoded para `strings.xml` (PR #15 + PR #21)
- [x] Strings XML layouts/menus extraídas + acessibilidade corrigida (PR #21)
- [x] Remover ~47 recursos não utilizados (Lint `UnusedResources`) — PR #22
- [x] **Cleanup pós-migração** — remover `proximoId()` + refatorar `InserirDesejoActivity` — PR #46
- [x] **Schema v9** — `ON DELETE CASCADE` nas FKs de `exclusao` e `desejo` — PR #47
- [x] **Coroutines** — `viewModelScope` + `Dispatchers.IO` no ViewModel — PR #48
- [x] `kotlin.random.Random` em `SorteioEngine` — PR #53
- [ ] Testes de UI com Espresso adicionais (GruposActivity, mais casos ParticipantesActivity)
- [ ] Logs estruturados (Timber)

### UI/UX
- [ ] Modo escuro completo (suporte parcial em `values-night/`)
- [ ] Suporte a tablets
- [ ] Transições entre Activities com shared elements

### Funcionalidades ← **#5**
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
