# Análise Técnica — AmigoSecreto Android

**Data:** Março/2026 (última atualização: 14/03/2026 — PR #27 merged)
**Versão Analisada:** 2.0 (build ~157+)
**Analista:** Revisão Senior Mobile

---

## Veredicto Geral

O app **está se aproximando do nível profissional** com progresso significativo desde a análise inicial.

É um app funcional, com pipeline de CI/CD real, testes unitários (224 casos), segurança bem configurada (HTTPS, queries parametrizadas, ProGuard), arquitetura MVVM com Repository pattern implementados. As Fases 1, 2, 3, 4 e 5 do roadmap foram concluídas. O PR #21 finalizou a extração de strings e acessibilidade.

O app está em nível profissional. O `lintDebug` está zerado — PR #22 eliminou os 47 `UnusedResources` e PR #27 eliminou os 9 `Overdraw`. O próximo passo é Dependency Injection (Hilt).

---

## O que Está Bem

| Área | Avaliação |
|------|-----------|
| CI/CD (GitHub Actions → Play Store) | Excelente |
| Segurança (HTTPS, SQL injection, keystore) | Sólido |
| Testes unitários e de DAO (Robolectric) | **172 casos — PR #18** |
| `MensagemSecretaBuilder` — lógica de mensagem extraída | **PR #17** |
| `ParticipantesViewModel` — sorteio e carregamento em background | **PR #18** |
| Feedback visual e haptico | Bom |
| EdgeToEdge / WindowInsets | Corrigido no PR #12 |
| Formatação monetária pt-BR | Corrigida no PR #12 |
| Layout responsivo de participantes | Corrigido no PR #12 |
| Utilitário centralizado (WindowInsetsUtils) | Implementado no PR #12 |
| Lint crítico zerado | **Resolvido no PR #14** |
| Strings hardcoded → strings.xml | **~150 extraídas no PR #15, layouts no PR #21** |
| Bug equals/hashCode em Desejo | **Corrigido no PR #16** |

---

## O que Precisa Melhorar

### 1. Arquitetura — Crítico

**Problema:** Activity-based com lógica de negócio misturada na camada de UI.

- `ParticipantesActivity` tem mais de 500 linhas combinando: UI, sorteio, SMS, formatação de mensagem, navegação.
- Sem ViewModel → rotação de tela reinicia operações pesadas.
- Sem Repository → Activities acessam DAO diretamente.

**Impacto:** Dificulta testes, manutenção e evolução do código.

**Solução:** Migrar para MVVM + ViewModel + LiveData/Flow. Ver Roadmap §1.

---

### 2. Strings Hardcoded — ✅ CONCLUÍDO (PR #15 + PR #21)

**Resolvido:** ~150 strings Java extraídas (PR #15) + todas as strings XML de layouts e menus extraídas (PR #21). `lintRelease` passa sem nenhum `HardcodedText`.

PR #21 adicionou também:
- `tools:text` em lugar de `android:text` para valores de preview do Android Studio em RecyclerView items
- Emojis decorativos em `strings.xml` com `translatable="false"`
- `android:importantForAccessibility="no"` em ícones puramente decorativos
- Novos drawables `ic_add`, `ic_dice`, `ic_arrow_forward` (substitui `›` tipográfico, RTL-safe)

---

### 3. Recursos Não Utilizados — ✅ CONCLUÍDO (PR #22)

**Resolvido:** 47 → 0 warnings `UnusedResources` no `lintDebug`.

PR #22 removeu 5 arquivos genuinamente orphans (`bounce.xml`, `button_press.xml`, `icon.png`, `inicial_icon.png`, `splash.xml`) e dezenas de cores, strings, estilos e dimens não utilizados. False positives (valores de `values-night/` e `values-w820dp/`) suprimidos com `tools:ignore="UnusedResources"` granular + comentários explicativos. Dependência `core-splashscreen:1.0.1` removida do `build.gradle`.

---

### 4. Cobertura de Testes — ✅ CONCLUÍDO (PR #16)

**Resolvido:** 132 testes cobrindo todas as camadas de DAO e model.

**Adicionados no PR #16:**
- `DesejoDAOTest` — 23 casos: CRUD completo, edge cases (IDs inexistentes, falha de inserção), isolamento via `limparTudo()`
- `DesejoModelTest` — 18 casos: construtores, equals/hashCode, compareTo, toString, campos null
- Bug fix em `Desejo.java`: `participanteId` adicionado a `equals()` e `hashCode()`

**Adicionados no PR #17:**
- `MensagemSecretaBuilderTest` — 22 casos: nomes nulos, sem desejos, com desejos, categoria em branco, lojas, faixas de preço, produto nulo/vazio, múltiplos itens
- Bug fix: lógica de faixa inválida (`min > max`) — exibe `"ate R$ max"` corretamente em vez de `"a partir de"`
- Refactor: `gerarMensagemSecreta()` removida de `ParticipantesActivity`, 3 call sites migrados para `MensagemSecretaBuilder.gerar()` diretamente

**Adicionados no PR #18:**
- `ParticipantesViewModel` — `AndroidViewModel` com LiveData; `carregarParticipantes()` e `realizarSorteio()` em background thread
- `ParticipantesViewModelTest` — 11 casos: Robolectric + `InstantTaskExecutorRule` + executor síncrono
- `ParticipantesActivity` virou observadora: `atualizarLista()` e `realizarSorteio()` delegam ao ViewModel
- Rotação de tela não reinicia mais o sorteio

**Adicionados no PR #19–#20:**
- `ParticipanteRepository` / `DesejoRepository` desacoplando ViewModel dos DAOs
- `ParticipanteRepositoryTest` — 17 casos, `DesejoRepositoryTest` — 16 casos
- Cobertura de caminhos de erro no ViewModel (total: 224 testes)

---

### 5. Problemas Menores de Lint — ✅ CONCLUÍDO (PR #14)

**Resolvido:** Lint crítico zerado. `lintRelease` passa sem novos erros.

Itens corrigidos no PR #14:
- Cursor leak em `DesejoDAO` — fechado em `finally` block
- `DefaultLocale` em `String.format()` — `Locale.getDefault()` aplicado
- Overdraw — backgrounds duplicados removidos

Warnings residuais em `lintDebug` (não bloqueadores — `lintRelease` zerado):
- ~~47 `UnusedResources`~~ — ✅ zerado no PR #22
- ~~9 `Overdraw`~~ — ✅ zerado no PR #27
- Outros menores (`GradleDependency`, `UseCompoundDrawables`, etc.)

---

### 6. Limitação Conhecida — Baixo

`marcarComoEnviado()` em `ParticipantesActivity` é chamado **antes** do usuário confirmar o share sheet. A API `ACTION_SEND` não oferece callback de confirmação, então se o usuário abrir o chooser e cancelar, o participante fica marcado como "enviado" sem que a mensagem tenha sido enviada.

Documentado no `CLAUDE.md`. Sem solução perfeita na API atual — mitigável com confirmação via dialog antes de marcar.

---

## Roadmap de Melhorias

### Progresso

| Fase | Descrição | Status | PR |
|------|-----------|--------|----|
| Fase 5 | Fixes de Lint Críticos | ✅ Concluído | #14 |
| Fase 2 | Strings Hardcoded → strings.xml (Java) | ✅ Concluído | #15 |
| Fase 3 | Limpeza de Recursos Não Utilizados | ✅ Concluído | #14/#15/#22 |
| Fase 4 | Cobertura de Testes (DAOs, Models, Repos, ViewModel) | ✅ Concluído | #16–#20 |
| Fase 1 | Arquitetura MVVM + Repository pattern | ✅ Concluído | #17–#20 |
| Fase 6 | Strings XML layouts/menus + Acessibilidade | ✅ Concluído | #21 |
| **Fase 7** | **UnusedResources (47 warnings lintDebug)** | **✅ Concluído** | **#22** |
| **Fase 8** | **Overdraw residual (9 warnings lintDebug)** | **✅ Concluído** | **#27** |
| **Fase 9** | **Dependency Injection (Hilt)** | **⏳ Próximo** | — |

---

### Fase 1 — Arquitetura — ✅ Concluído (PR #17–#20)

**Objetivo:** Separar responsabilidades, tornar o código testável e preparar para evolução futura.

```
MensagemSecretaBuilder.java       ← ✅ PR #17 — puro Java, testável
ParticipantesViewModel.java       ← ✅ PR #18 — MVVM + LiveData
ParticipanteRepository.java       ← ✅ PR #19 — desacoplado do DAO
DesejoRepository.java             ← ✅ PR #19
    ↓
LiveData → Activity observa e atualiza UI ← ✅ PR #18
```

- [x] Extrair `MensagemSecretaBuilder.java` + 22 testes — PR #17
- [x] `ParticipantesViewModel` com LiveData + 31 testes — PR #18
- [x] `ParticipanteRepository` / `DesejoRepository` + 33 testes — PR #19
- [x] Cobertura de caminhos de erro + testes ViewMode adicionais — PR #20

---

### Fase 2 — Strings Hardcoded — ✅ Concluído (PR #15 + PR #21)

~150 textos Java extraídos (PR #15) + todos os layouts/menus extraídos (PR #21). `lintRelease` zerado em `HardcodedText`.

---

### Fase 3 — Limpeza de Recursos — ✅ Concluído (PR #14/#15/#22)

Recursos críticos removidos no PR #14/#15. PR #22 zerou os 47 `UnusedResources` restantes no `lintDebug`.

---

### Fase 4 — Cobertura de Testes — ✅ Concluído (PR #16–#20)

224 testes. Todas as camadas cobertas: DAO, model, repository, ViewModel, utilitários. Ver seção §4 acima.

---

### Fase 5 — Fixes de Lint Críticos — ✅ Concluído (PR #14)

Cursor leak, `DefaultLocale`, Overdraw crítico — resolvidos. `lintRelease` sem erros bloqueadores.

---

### Fase 6 — Strings XML Layouts/Menus + Acessibilidade — ✅ Concluído (PR #21)

- Todos os `android:text`, `android:contentDescription`, `android:title` em layouts e menus extraídos para `strings.xml`
- `tools:text` para preview-only values em RecyclerView items
- Emojis decorativos com `translatable="false"`
- `android:importantForAccessibility="no"` em ícones decorativos
- Setas tipográficas `›` substituídas por `ic_arrow_forward` (RTL-safe, `autoMirrored="true"`)
- Emojis removidos dos labels de `MaterialButton`, substituídos por `app:icon` com SVG

---

### Fase 7 — UnusedResources — ✅ Concluído (PR #22)

**Objetivo:** Zerar os 47 warnings `UnusedResources` no `lintDebug`. ✅

**Resultado:**
- 5 arquivos genuinamente orphans deletados: `bounce.xml`, `button_press.xml`, `icon.png`, `inicial_icon.png`, `splash.xml`
- Dezenas de cores (`purple_medium/light`, gradientes), strings, estilos (`AppTheme.Button.*`, `AppTheme.Card`) e dimens removidos
- False positives de `values-night/` e `values-w820dp/` suprimidos com `tools:ignore="UnusedResources"` granular
- Dependência `core-splashscreen:1.0.1` removida do `build.gradle`
- 3 layouts reservados mantidos com `tools:ignore` + issues #23/#24/#25 para rastreamento

---

### Fase 8 — Overdraw Residual — ✅ Concluído (PR #27)

**Objetivo:** Zerar os 9 warnings `Overdraw` no `lintDebug`. ✅

**Causa:** Todos os 9 eram `android:background="@color/background"` no root element de layouts de Activity. O tema `Theme.AmigoSecreto` já define `android:windowBackground` com a mesma cor — double paint desnecessário.

**Solução:** Remover o atributo redundante dos 9 layouts afetados. `lintDebug Overdraw`: 9 → 0.

---

### Fase 9 — Dependency Injection com Hilt (Próximo)

**Objetivo:** Introduzir Hilt para injeção de dependências, eliminando acoplamento manual entre ViewModel, Repository e DAO.

**Escopo:**
- Adicionar dependências `hilt-android` e `hilt-compiler` ao `build.gradle`
- Anotar `Application`, Activities e ViewModels com `@HiltAndroidApp` / `@AndroidEntryPoint` / `@HiltViewModel`
- Criar módulos `@Module` para prover `MySQLiteOpenHelper`, DAOs e Repositories
- Remover instanciação manual em `ParticipantesViewModel`

**Pré-requisito:** Migração para Kotlin (Hilt é mais ergonômico com Kotlin; funciona em Java mas com mais boilerplate).

---

## Histórico de Melhorias por PR

### PR #12 — UI/UX e EdgeToEdge

| Problema | Antes | Depois |
|----------|-------|--------|
| Layout truncado em telas pequenas | Cards quebravam | Layout 2 linhas responsivo |
| Layout quebra após compartilhar | SMS abria e cortava layout | Corrigido (EdgeToEdge + IME insets) |
| Mensagem SMS incoerente | Dizia "Role para baixo" sem sentido | Mensagem limpa e direta |
| Formatação monetária incorreta | `1500` (sem separador) | `1.500,00` (pt-BR correto) |
| Botão sumia com teclado aberto | Botão ficava atrás do teclado | IME padding aplicado |
| Sobreposição da status bar | AppBar coberta parcialmente | `fitsSystemWindows` em todas as telas |

### PR #14 — Lint e Qualidade de Código

| Item | Situação |
|------|---------|
| Cursor leak em `DesejoDAO` | Fechado em `finally` block |
| `DefaultLocale` em `String.format()` | `Locale.getDefault()` aplicado |
| Overdraw em views aninhadas | Backgrounds duplicados removidos |
| Recursos não utilizados (~40) | Removidos |
| `lintRelease` | Passa sem novos erros |

### PR #15 — Strings Hardcoded

| Item | Situação |
|------|---------|
| Strings hardcoded em Java (~120) | Extraídas para `strings.xml` |
| Strings hardcoded em XML (~30) | Extraídas para `strings.xml` |
| Internacionalização (i18n) | Habilitada — base para pt-BR e en-US |

### PR #16 — Testes e Bug Fix

| Item | Situação |
|------|---------|
| `DesejoDAOTest` — 23 casos | Criado |
| `DesejoModelTest` — 18 casos | Criado |
| Bug `equals`/`hashCode` em `Desejo` | Corrigido — `participanteId` incluído |
| Total de testes | 132 (era 91 antes do PR) |

### PR #17 — MensagemSecretaBuilder (Fase 1, etapa 1)

| Item | Situação |
|------|---------|
| `MensagemSecretaBuilder.java` — classe pura Java extraída | Criado |
| `MensagemSecretaBuilderTest` — 22 casos | Criado |
| Bug fix: faixa de preço inválida (`min > max`) exibia `"a partir de"` | Corrigido |
| Wrapper `gerarMensagemSecreta()` removido de `ParticipantesActivity` | Refatorado |
| Total de testes | 154 (era 132 antes do PR) |

### PR #18–#20 — MVVM + Repository + Testes

| Item | Situação |
|------|---------|
| `ParticipantesViewModel` + LiveData | Criado (PR #18) |
| `ParticipanteRepository` / `DesejoRepository` | Criado (PR #19) |
| Testes: ViewModel (31), Repository (33), error paths | Criados (PR #18–#20) |
| Total de testes | 224 (era 154 antes do PR #18) |

### PR #21 — Strings XML Layouts/Menus + Acessibilidade

| Item | Situação |
|------|---------|
| Strings hardcoded em layouts e menus extraídas | ~65 novas chaves em `strings.xml` |
| `tools:text` para previews em RecyclerView items | 11 atributos corrigidos |
| Emojis com `translatable="false"` | 6 chaves `emoji_*` |
| Prefixo `cd_` exclusivo para `contentDescription` | 8 chaves renomeadas |
| Ícones decorativos com `importantForAccessibility="no"` | 12+ views corrigidas |
| Setas `›` → `ic_arrow_forward` (RTL-safe) | 4 substituições |
| `lintRelease` `HardcodedText` | Zerado |

### PR #22 — UnusedResources Cleanup (Fase 7)

| Item | Situação |
|------|---------|
| Arquivos orphans deletados | `bounce.xml`, `button_press.xml`, `icon.png`, `inicial_icon.png`, `splash.xml` |
| Cores removidas | `purple_medium/light/ultra_light`, gradientes de card, legacy orphans |
| Strings removidas | ~15 strings orphans (labels, helpers, títulos) |
| Estilos removidos | `AppTheme.Button.Primary/Secondary`, `AppTheme.Card` |
| False positives suprimidos | `tools:ignore` granular com comentários em `colors.xml`, `dimens.xml`, `strings.xml`, 3 layouts |
| Dependência removida | `core-splashscreen:1.0.1` |
| Issues abertas | #23 (`empty_state.xml`), #24 (`loading_state.xml`), #25 (`item_grupo.xml`) |
| `lintDebug UnusedResources` | 47 → 0 |
| Testes | 224 (sem regressão) |

### PR #27 — Overdraw Cleanup (Fase 8)

| Item | Situação |
|------|---------|
| `android:background="@color/background"` redundante removido | 9 layouts de Activity e helper |
| Causa raiz | `@color/background → @color/md_theme_background`; tema já define `android:windowBackground` com o mesmo valor |
| Arquivos corrigidos | `activity_alterar_desejo.xml`, `activity_detalhe_desejo.xml`, `activity_inserir_desejo.xml`, `activity_listar_desejos.xml`, `activity_listar_participantes.xml`, `activity_participante_desejos.xml`, `activity_revelar_amigo.xml`, `activity_visualizar_desejos.xml`, `loading_state.xml` |
| `lintDebug Overdraw` | 9 → 0 |
| Testes | 224 (sem regressão) |

---

## Conclusão

O app atingiu nível profissional. Todas as fases principais foram concluídas:
- 224 testes cobrindo DAOs, models, repositories, ViewModel e utilitários
- MVVM com Repository pattern implementado
- `lintRelease` sem erros bloqueadores
- `lintDebug` zerado — `UnusedResources` (PR #22) e `Overdraw` (PR #27)
- Strings organizadas em `strings.xml`, acessibilidade corrigida
- CI/CD funcional com deploy automático para Play Store

**Próximo passo:** Fase 9 — Dependency Injection com Hilt.
