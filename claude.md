# AmigoSecreto - Documentação do Projeto

## Visão Geral

**AmigoSecreto** é um aplicativo Android que facilita a organização e gerenciamento de amigo secreto. O app permite gerenciar participantes, realizar sorteios aleatórios, revelar resultados de forma interativa e compartilhar via WhatsApp. Inclui também uma funcionalidade de lista de desejos.

**Versão Atual**: 1.7 (versionCode: 7)
**Package**: activity.amigosecreto
**SDK Mínimo**: 21 (Android 5.0)
**SDK Alvo**: 34 (Android 14)

---

## Estrutura do Projeto

```
app/src/main/java/activity/amigosecreto/
├── ParticipantesActivity.java          # Tela principal - gerenciar participantes
├── RevelarAmigoActivity.java           # Revelar quem é o amigo secreto
├── ListarDesejos.java                  # Listar desejos/presentes
├── InserirDesejoActivity.java          # Adicionar novo desejo
├── AlterarDesejoActivity.java          # Editar desejo existente
├── DetalheDesejoActivity.java          # Detalhes do desejo
├── SplashActivity.java                 # Tela de splash
└── db/
    ├── MySQLiteOpenHelper.java         # Gerenciamento do banco SQLite
    ├── Participante.java               # Model de participante
    ├── ParticipanteDAO.java            # DAO para operações de participantes
    ├── Desejo.java                     # Model de desejo
    └── DesejoDAO.java                  # DAO para operações de desejos
```

---

## Funcionalidades Principais

### 1. Gerenciamento de Participantes
- **Adicionar participantes**: Manual ou importar dos contatos
- **Remover participantes**: Individual ou limpar todos
- **Visualizar lista**: Com status de envio e informações de contato
- **Validação**: Mínimo de 3 participantes para sorteio

### 2. Sorteio de Amigo Secreto
- **Algoritmo**: Embaralhamento aleatório com validação
- **Garantia**: Ninguém tira a si mesmo
- **Persistência**: Resultados salvos no banco de dados
- **Lógica do sorteio** em `ParticipantesActivity.java:284-300`

### 3. Revelação de Resultados
- **Interface interativa**: Toque para revelar
- **Proteção contra spoilers**: Layout escondido até o toque
- **Design Material**: CardView com animações
- **Implementação** em `RevelarAmigoActivity.java`

### 4. Compartilhamento via WhatsApp
- **Proteção anti-spoiler**: 30 linhas em branco antes da revelação
- **Integração direta**: API do WhatsApp quando tem telefone
- **Fallback**: Intent genérico de compartilhamento
- **Rastreamento**: Marca como "enviado" após compartilhar

### 5. Lista de Desejos
- **Cadastro completo**: Produto, categoria, faixa de preço, lojas
- **Gerenciamento**: Adicionar, editar, remover desejos
- **Compartilhamento**: Lista completa formatada
- **Integração BuscaPé**: Busca de preços externa

---

## Banco de Dados

### Nome: `amigosecreto_new.db` (versão 5)

#### Tabela: `participante`
```sql
CREATE TABLE participante (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome TEXT NOT NULL,
    email TEXT,
    telefone TEXT,
    amigo_sorteado_id INTEGER,
    enviado INTEGER DEFAULT 0
)
```

**Campos**:
- `id`: Identificador único
- `nome`: Nome do participante (obrigatório)
- `email`: Email (opcional)
- `telefone`: Telefone (opcional)
- `amigo_sorteado_id`: ID do participante sorteado para este dar presente
- `enviado`: Flag se o resultado foi compartilhado (0=não, 1=sim)

#### Tabela: `desejo`
```sql
CREATE TABLE desejo (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    produto TEXT NOT NULL,
    categoria TEXT,
    preco_minimo REAL,
    preco_maximo REAL,
    lojas TEXT
)
```

**Campos**:
- `id`: Identificador único
- `produto`: Nome do produto (obrigatório)
- `categoria`: Categoria do produto
- `preco_minimo`: Preço mínimo desejado
- `preco_maximo`: Preço máximo desejado
- `lojas`: Lojas sugeridas

---

## Tecnologias Utilizadas

### Build
- **Gradle**: 7.x
- **Android Gradle Plugin**: 8.7.0
- **Java**: Compatibilidade com Java 8

### Bibliotecas AndroidX
```gradle
dependencies {
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.9.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
}
```

### Recursos Android
- **SQLite**: Banco de dados local via SQLiteOpenHelper
- **Material Design 3**: Componentes modernos de UI
- **Intent API**: Navegação e compartilhamento
- **Contacts Provider**: Importação de contatos

---

## Permissões Necessárias

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.VIBRATE" />
<uses-permission android:name="android.permission.READ_CONTACTS" />
```

- **INTERNET**: Integração com WhatsApp e BuscaPé
- **ACCESS_NETWORK_STATE**: Verificação de conectividade
- **VIBRATE**: Feedback háptico
- **READ_CONTACTS**: Importar participantes dos contatos

---

## Padrões de Arquitetura

### DAO Pattern (Data Access Object)
- **ParticipanteDAO**: Operações CRUD de participantes
- **DesejoDAO**: Operações CRUD de desejos
- Separação clara entre lógica de negócio e acesso a dados

### Model Classes
- **Participante**: Implementa `Serializable` para passar via Intent
- **Desejo**: Implementa `Parcelable` para eficiência

### Activity-based Architecture
- Navegação tradicional via Intents
- Cada tela = uma Activity
- Dados passados via Intent extras

---

## Fluxo do Usuário

```
Iniciar App
    ↓
[ParticipantesActivity] - Tela Principal
    ├─ Adicionar Participantes
    │   ├─ Manualmente (dialog)
    │   └─ Importar dos Contatos
    ├─ Realizar Sorteio
    │   ├─ Validação (≥3 participantes)
    │   ├─ Algoritmo de embaralhamento
    │   └─ Salvar resultados
    ├─ Tocar em Participante
    │   └─ [RevelarAmigoActivity]
    │       └─ Tocar para revelar amigo
    └─ Compartilhar via WhatsApp
        └─ Proteção anti-spoiler

Menu Lateral: Lista de Desejos
    [ListarDesejos]
        ├─ Adicionar Novo → [InserirDesejoActivity]
        ├─ Ver Detalhes → [DetalheDesejoActivity]
        │   └─ Editar → [AlterarDesejoActivity]
        └─ Compartilhar Lista
```

---

## Detalhes de Implementação Notáveis

### Algoritmo de Sorteio
**Localização**: `ParticipantesActivity.java:284-300`

```java
// Garante que ninguém tira a si mesmo
boolean valido = false;
while (!valido) {
    Collections.shuffle(sorteados);
    valido = true;
    for (int i = 0; i < listaParticipantes.size(); i++) {
        if (listaParticipantes.get(i).getId() == sorteados.get(i).getId()) {
            valido = false;
            break;
        }
    }
}
```

### Compartilhamento WhatsApp com Anti-Spoiler
**Localização**: `ParticipantesActivity.java:445-465`

```java
StringBuilder sb = new StringBuilder();
sb.append("🎁 Resultado do Amigo Secreto 🎁\n\n");
sb.append("Oi ").append(participante.getNome()).append("!\n\n");

// 30 linhas em branco para proteção anti-spoiler
for (int i = 0; i < 30; i++) {
    sb.append(".\n");
}

sb.append("Seu amigo secreto é:\n\n");
sb.append("🎅 ").append(nomeAmigo).append(" 🎅\n\n");

// Link direto do WhatsApp se tiver telefone
String url = "https://api.whatsapp.com/send?phone=" + telefone
    + "&text=" + URLEncoder.encode(mensagem, "UTF-8");
```

### Importação de Contatos
**Localização**: `ParticipantesActivity.java:341-357`

```java
// Usa o seletor nativo de contatos
Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
startActivityForResult(intent, PICK_CONTACT);

// Processa resultado com ContentResolver
Cursor cursor = getContentResolver().query(contactUri, null, null, null, null);
// Extrai nome e telefone do contato selecionado
```

---

## Esquema de Cores

**Paleta**: Indigo & Emerald Professional

| Nome | Hex | Uso |
|------|-----|-----|
| colorPrimary | #4F46E5 | Cor primária (Indigo) |
| colorPrimaryDark | #3730A3 | Variante escura |
| colorAccent | #10B981 | Destaque/sucesso (Emerald) |
| background | #F9FAFB | Fundo de telas |
| text_primary | #111827 | Texto principal |
| text_secondary | #6B7280 | Texto secundário |
| error | #EF4444 | Ações destrutivas |
| success | #10B981 | Feedback positivo |

---

## Comandos Úteis

### Build e Instalação
```bash
# Build debug
./gradlew assembleDebug

# Instalar no dispositivo
./gradlew installDebug

# Build e instalar
./gradlew installDebug

# Limpar build
./gradlew clean
```

### Logs
```bash
# Ver logs do app
adb logcat | grep -i "amigosecreto"

# Ver logs de crash
adb logcat | grep -E "AndroidRuntime|FATAL"
```

### Banco de Dados (Debug)
```bash
# Acessar banco no emulador/dispositivo root
adb shell
run-as activity.amigosecreto
cd databases
sqlite3 amigosecreto_new.db

# Comandos SQLite úteis
.tables                    # Listar tabelas
.schema participante       # Ver estrutura da tabela
SELECT * FROM participante; # Consultar dados
```

---

## Próximas Melhorias Sugeridas

### Arquitetura
- [ ] Migrar para MVVM com ViewModel e LiveData
- [ ] Implementar Repository pattern
- [ ] Usar Coroutines para operações assíncronas
- [ ] Adicionar Dependency Injection (Hilt/Koin)

### UI/UX
- [ ] Substituir ListView por RecyclerView
- [ ] Implementar ViewBinding/DataBinding
- [ ] Adicionar animações de transição
- [ ] Modo escuro (Dark Theme)
- [ ] Suporte a tablets (layout responsivo)

### Funcionalidades
- [ ] Backup/restore de dados (Google Drive)
- [ ] Histórico de sorteios anteriores
- [ ] Notificações de lembrete
- [ ] Compartilhar via Telegram/Email
- [ ] QR Code para compartilhamento rápido
- [ ] Limite de preço por grupo
- [ ] Restrições de quem não pode tirar quem

### Qualidade
- [ ] Testes unitários (JUnit)
- [ ] Testes de UI (Espresso)
- [ ] CI/CD com GitHub Actions
- [ ] Análise de código (SonarQube/Lint)
- [ ] Tratamento de erros robusto
- [ ] Logs estruturados (Timber)

### Performance
- [ ] Paginação na lista de participantes
- [ ] Cache de imagens de contatos
- [ ] Otimização de queries SQL
- [ ] ProGuard/R8 para release

---

## Contato e Contribuição

**Repositório**: https://github.com/Vitorspk/amigosecreto
**Commits Recentes**:
- "Add comprehensive English README documentation"
- "Claude Code Review workflow"
- "Claude PR Assistant workflow"

**Branch Principal**: `master`

---

## Licença

(Adicionar informações de licença conforme necessário)