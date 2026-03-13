# Amigo Secreto

Aplicativo Android para organizar sorteios de amigo secreto com compartilhamento via WhatsApp e SMS, lista de desejos por participante e revelação interativa com proteção anti-spoiler.

[![CI](https://github.com/Vitorspk/amigosecreto/actions/workflows/ci.yml/badge.svg)](https://github.com/Vitorspk/amigosecreto/actions/workflows/ci.yml)
[![Release](https://github.com/Vitorspk/amigosecreto/actions/workflows/release.yml/badge.svg)](https://github.com/Vitorspk/amigosecreto/actions/workflows/release.yml)

**Package Play Store:** `com.amigosecreto.sorteio` | **Branch principal:** `master` | **Min SDK:** 21 (Android 5.0+)

---

## Funcionalidades

- **Múltiplos grupos** — organize sorteios separados por evento ou família
- **Participantes** — adicione manualmente ou importe direto da agenda de contatos
- **Restrições** — defina quem não pode tirar quem antes do sorteio
- **Sorteio automático** — algoritmo com validação de restrições e transação atômica (ninguém tira a si mesmo)
- **Revelação interativa** — cada participante revela seu resultado com proteção anti-spoiler
- **Compartilhamento** — WhatsApp (30 linhas em branco anti-spoiler) e SMS via intent nativo
- **Lista de desejos** — produto, categoria, faixa de preço e lojas sugeridas por participante
- **Busca de produtos** — integração com Buscape via HTTPS

---

## Stack

| Componente | Tecnologia |
|------------|------------|
| Linguagem | Java 17 |
| Min SDK | 21 (Android 5.0 Lollipop) |
| Target / Compile SDK | 35 (Android 15) |
| UI | Material Design 3 + Edge-to-Edge |
| Banco de dados | SQLite (schema v8) |
| Build | Android Gradle Plugin 9.0.1 + ViewBinding + MultiDex |
| Ofuscação | R8 / ProGuard (release) |
| Testes | JUnit 4 + Mockito + Robolectric 4.13 + Espresso |
| CI/CD | GitHub Actions → Google Play |

---

## Pré-requisitos

- Android Studio Iguana ou superior
- JDK 17+
- Para builds de release: arquivo `keystore.properties` na raiz (ver `keystore.properties.template`)

---

## Setup local

```bash
# Clonar o repositório
git clone https://github.com/Vitorspk/amigosecreto.git
cd amigosecreto

# Build debug
./gradlew assembleDebug

# Instalar no dispositivo/emulador
./gradlew installDebug

# Build release AAB (requer keystore.properties)
./gradlew bundleRelease

# Limpar build
./gradlew clean
```

---

## Testes

```bash
# Unitários + Robolectric (sem emulador — rápido)
./gradlew :app:testDebugUnitTest

# Instrumentados com Espresso (requer emulador ou dispositivo)
./gradlew :app:connectedDebugAndroidTest

# Lint
./gradlew :app:lintRelease
```

**Cobertura atual:** 91 testes — modelos, DAOs, motor de sorteio e validações. Ver [`documents/TEST_PLAN.md`](documents/TEST_PLAN.md).

| Camada | Arquivo | Casos |
|--------|---------|------:|
| Util | `ValidationUtilsTest` | 14 |
| Util | `SorteioEngineTest` | 11 |
| Util | `FormatarPrecoTest` | 9 |
| Model | `GrupoModelTest` | 7 |
| Model | `ParticipanteModelTest` | 11 |
| DAO | `GrupoDAOTest` | 12 |
| DAO | `ParticipanteDAOTest` | 20 |
| DAO | `MySQLiteOpenHelperTest` | 7 |

---

## CI/CD

| Trigger | Track | versionName |
|---------|-------|-------------|
| Push em `master` | Internal (QA) | `2.0-dev.<short-sha>` |
| Tag `v*` (ex: `v2.1`) | Production | extraído da tag |

`versionCode` = `100 + git rev-list --count HEAD` (ambos os ambientes).

### Deploy para produção

```bash
git tag v2.1
git push origin v2.1
```

Workflow `release.yml`: checkout → JDK 21 → lint → testes → bundleRelease → Play Store (production) → GitHub Release.

### Deploy para track interno (QA)

```bash
git push origin master
```

Workflow `ci.yml`: mesmos passos, track **internal**, `cancel-in-progress: true`.

### Signing config

O `build.gradle` suporta dois modos:
- **Local:** lê de `keystore.properties` (não commitado)
- **CI:** lê de environment variables (`CI_KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, etc.)

### GitHub Secrets necessários

| Secret | Descrição |
|--------|-----------|
| `KEYSTORE_BASE64` | Keystore codificada em base64 |
| `KEYSTORE_PASSWORD` | Senha do keystore |
| `KEY_ALIAS` | Alias da chave de assinatura |
| `KEY_PASSWORD` | Senha da chave |
| `PLAY_STORE_SERVICE_ACCOUNT_JSON` | JSON da service account do Google Play |

---

## Estrutura do projeto

```
app/src/main/java/activity/amigosecreto/
├── GruposActivity.java                    # LAUNCHER — tela principal
├── ParticipantesActivity.java             # gerenciar participantes e sorteio
├── RevelarAmigoActivity.java              # revelar resultado interativamente
├── ParticipanteDesejosActivity.java       # desejos de um participante específico
├── VisualizarDesejosActivity.java         # todos os desejos de um grupo
├── ListarDesejosActivity.java             # lista de desejos geral
├── InserirDesejoActivity.java             # adicionar desejo
├── AlterarDesejoActivity.java             # editar desejo
├── DetalheDesejoActivity.java             # detalhes do desejo + busca Buscape
│
├── adapter/
│   └── ParticipantesRecyclerAdapter.java  # RecyclerView com ViewHolder pattern
│
├── db/
│   ├── MySQLiteOpenHelper.java            # schema SQLite v8 + migrações
│   ├── Grupo.java / GrupoDAO.java         # model + CRUD de grupos
│   ├── Participante.java / ParticipanteDAO.java  # model + CRUD + sorteio + exclusões
│   └── Desejo.java / DesejoDAO.java       # model + CRUD de desejos
│
└── util/
    ├── AnimationUtils.java                # animações reutilizáveis
    ├── AsyncDatabaseHelper.java           # operações assíncronas no banco
    ├── HapticFeedbackUtils.java           # feedback háptico (respeita acessibilidade)
    ├── SnackbarHelper.java                # mensagens padronizadas
    ├── SorteioEngine.java                 # motor de sorteio (extraído para testabilidade)
    ├── ValidationUtils.java               # validação de inputs
    └── WindowInsetsUtils.java             # IME padding, locale pt-BR, formatação monetária

.github/workflows/
├── release.yml                            # deploy automático para Play Store (tag v*)
├── ci.yml                                 # CI + deploy internal track (push master)
├── claude-code-review.yml                 # review automático de PRs
└── pr-checks.yml                          # validações de PR

distribution/whatsnew/
├── whatsnew-pt-BR                         # release notes em português
└── whatsnew-en-US                         # release notes em inglês

documents/
├── TECHNICAL_ANALYSIS.md                  # análise técnica e roadmap
├── TEST_PLAN.md                           # plano de testes (Fases 1–3)
└── RELEASE_INSTRUCTIONS.md               # processo de release detalhado
```

---

## Banco de dados

Schema `amigosecreto_v8.db` com 4 tabelas:

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

**Migrações:**
- `< v7` → drop e recria tudo
- `v7 → v8` → adiciona coluna `participante_id` na tabela `desejo`

**Inspecionar em debug:**
```bash
adb shell
run-as com.amigosecreto.sorteio.debug
cd databases && sqlite3 amigosecreto_v8.db
.tables
SELECT * FROM participante WHERE grupo_id = 1;
```

---

## Fluxo do usuário

```
[GruposActivity]  ← LAUNCHER
    ├── Criar / Editar / Remover Grupo
    └── Selecionar Grupo
            └── [ParticipantesActivity]
                    ├── Adicionar participante (manual ou contatos)
                    ├── Configurar restrições (exclusões)
                    ├── Realizar sorteio (mínimo 3 participantes)
                    ├── Compartilhar individual (WhatsApp / SMS)
                    ├── [RevelarAmigoActivity]
                    └── [VisualizarDesejosActivity]
                                └── [ParticipanteDesejosActivity]

Menu global
    └── [ListarDesejosActivity]
            ├── [InserirDesejoActivity]
            └── [DetalheDesejoActivity]
                        └── [AlterarDesejoActivity]
```

---

## Edge-to-Edge (Android 15)

Todas as 9 Activities chamam `EdgeToEdge.enable(this)` antes de `setContentView()`.

| Tela | Estratégia |
|------|------------|
| CoordinatorLayout + AppBarLayout | insets automáticos via `fitsSystemWindows` |
| `RevelarAmigoActivity` | listener manual no `root_revelar` |
| `ParticipantesActivity` | listener no `layout_bottom_buttons` com padding base capturado fora da lambda |
| `ListarDesejos` (FAB) | ajuste de `bottomMargin` via `requestLayout()` |
| Telas de desejo (`InserirDesejo`, `AlterarDesejo`) | `WindowInsetsUtils.applyImeBottomPadding()` — teclado não cobre o botão |

---

## Segurança

- `usesCleartextTraffic="false"` — somente HTTPS
- Network Security Config (`xml/network_security_config.xml`)
- Queries parametrizadas em todos os DAOs — prevenção de SQL injection
- ProGuard/R8: ofuscação + remoção de logs em release
- Keystore nunca commitada (`.gitignore` protege `*.keystore` e `keystore.properties`)
- GitHub Actions pinadas por SHA de commit — supply-chain security
- Backup rules configuradas (`xml/backup_rules.xml`, `xml/data_extraction_rules.xml`)

---

## Esquema de cores

**Paleta:** Indigo & Emerald

| Token | Hex | Uso |
|-------|-----|-----|
| `colorPrimary` | `#4F46E5` | Cor principal (Indigo) |
| `colorPrimaryDark` | `#3730A3` | Variante escura |
| `colorAccent` | `#10B981` | Destaque / sucesso (Emerald) |
| `background` | `#F9FAFB` | Fundo das telas |
| `text_primary` | `#111827` | Texto principal |
| `text_secondary` | `#6B7280` | Texto secundário |
| `error` | `#EF4444` | Ações destrutivas |

---

## Limitação conhecida

`marcarComoEnviado()` é chamado imediatamente ao abrir o share sheet — a API `ACTION_SEND` não oferece callback de confirmação. Se o usuário abrir o chooser e cancelar, o participante ficará marcado como "enviado" sem que a mensagem tenha sido enviada.

---

## Documentação

| Documento | Descrição |
|-----------|-----------|
| [`documents/TECHNICAL_ANALYSIS.md`](documents/TECHNICAL_ANALYSIS.md) | Análise técnica completa e roadmap de melhorias |
| [`documents/TEST_PLAN.md`](documents/TEST_PLAN.md) | Plano de testes (Fases 1–3) |
| [`documents/RELEASE_INSTRUCTIONS.md`](documents/RELEASE_INSTRUCTIONS.md) | Processo de release detalhado |
| [`PRIVACY_POLICY.md`](PRIVACY_POLICY.md) | Política de privacidade |
| [`PLAY_STORE_LISTING.md`](PLAY_STORE_LISTING.md) | Textos da listagem na Play Store |

---

## Contribuindo

1. Crie uma branch a partir de `master`: `git checkout -b feat/sua-feature`
2. Implemente com testes (mínimo 80% de cobertura)
3. Verifique: `./gradlew :app:testDebugUnitTest && ./gradlew :app:lintRelease`
4. Abra um Pull Request descrevendo o que foi alterado e por quê

---

## Licença

Projeto privado. Todos os direitos reservados.
