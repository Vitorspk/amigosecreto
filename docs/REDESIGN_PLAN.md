# Plano de redesign — Amigo Secreto

Criado em: 31/10/2026. Documento de acompanhamento obrigatório do redesign.

## Objetivo e contrato de execução

Modernizar todas as telas do aplicativo Android `com.amigosecreto.sorteio`, com identidade consistente, foco na organização do evento e uma revelação especial. Preservar grupos, participantes, desejos, restrições, histórico, backup, notificações e compartilhamento.

Antes de cada sessão: ler este documento, consultar `git status`, identificar a primeira etapa pendente e conferir as evidências existentes. Ao encerrar: atualizar status, arquivos, verificações executadas, limitações e próximo passo. Não marcar como `done` uma entrega apenas planejada, uma tela não conferida ou um teste que não passou. Alterações no escopo ou na ordem precisam ser justificadas aqui antes da execução. Trabalhar em branch própria; publicação é uma entrega separada.

Status: `todo` (não iniciado), `in_progress` (em execução), `blocked` (dependência identificada), `done` (critérios cumpridos com evidência).

## Diagnóstico inicial

- [x] Projeto localizado em `/Users/home/Documents/workspace-github/amigosecreto`.
- [x] Identidade confirmada no painel autenticado da Play Console: Amigo Secreto Sorteio QR Code, pacote `com.amigosecreto.sorteio`.
- [x] Stack conferida no código: Kotlin, Views/XML, Material 3, ViewBinding, Room, minSdk 24, compile/target 36. O README descreve uma versão antiga da arquitetura; usar os arquivos atuais como referência.
- [x] Inventário inicial: 15 Activities declaradas, 30 layouts, recursos próprios para modo escuro e telas auxiliares.
- [x] Problemas observáveis no código: gradientes multicoloridos competem com as ações; sombras fortes nos cards e textos; cabeçalhos e botões com estilos diferentes; emojis usados como ícones em excesso; tamanhos e cores definidos diretamente nas telas; contraste dependente de fundos coloridos.
- [x] Registrar capturas da interface anterior no emulador: `05-before-onboarding.png` e `06-before-home.png`. A inspeção XML ainda não substitui a avaliação do aplicativo em execução.

## Direção visual

**Revisão 2 — solicitada pelo usuário em 31/10/2026:** a primeira proposta foi considerada simples demais e inferior à produção. Os resultados técnicos anteriores permanecem históricos; R1–R7 estão reabertas e a aprovação funcional não substitui qualidade visual. Nova direção: identidade com azul profundo, violeta e detalhes dourados, ilustração vetorial própria de presentes, superfícies com profundidade discreta, cabeçalhos compostos, tipografia mais controlada, cards com metadados e painéis de métricas em duas colunas. Evitar grandes blocos vazios, ícones gigantes e telas compostas apenas por retângulos iguais. Preservar todos os fluxos recuperados e validar capturas reais antes de encerrar novamente.

**Conceito vigente: encontros que viram boas surpresas.** Azul profundo e violeta, detalhes dourados e ilustração vetorial de presentes nos destaques; superfícies frias, tipografia controlada e elevação discreta no conteúdo. Margens de 20 dp, cards com metadados e faixa de ações, cabeçalhos compostos e métricas em grade. O modo escuro conserva a hierarquia com superfícies azuladas. A revisão 1 usava marfim/ameixa e composição mais simples; permanece apenas como histórico.

Hierarquia: contexto do evento → conteúdo → próxima ação. Uma ação principal por fluxo, ações secundárias com menor peso. Preservar linguagem em português e feedback de carregamento, vazio, erro e sucesso. Não adicionar abas sem destino nem botões decorativos.

## Etapas e critérios de entrega

| ID | Etapa | Status | Critério de conclusão |
| --- | --- | --- | --- |
| R0 | Identificação, inventário e plano | done | Documento criado antes das alterações de UI; mapa das telas e capturas anteriores registrados |
| R1 | Sistema visual compartilhado | done | Paletas clara/escura, tipografia, botões, campos, cards, toolbar e espaçamento centralizados; recursos compilam |
| R2 | Entrada e grupos | done | Onboarding, lista, vazio, criação/edição e cards redesenhados; criar e abrir grupo conferidos |
| R3 | Evento e participantes | done | Lista, busca, inclusão manual/contato, restrições e configurações coerentes; ações acessíveis e sem sobreposição |
| R4 | Sorteio, revelação e compartilhamento | done | Confirmação, estados de revelação, histórico, folha de compartilhamento e QR coerentes; proteção anti-spoiler preservada |
| R5 | Desejos | done | Listas geral/por participante/por grupo, inserir, editar, detalhe e opções redesenhados; fluxo CRUD preservado |
| R6 | Resumos e estatísticas | done | Dashboard e estatísticas coerentes com o sistema visual; números e estados corretos |
| R7 | Validação e entrega | done | Build debug, testes unitários e lint; inspeção clara/escura, fonte ampliada e tela pequena; APK e evidências documentados |

Cada etapa pode receber implementação em lote, mas só fica `done` após cumprir sua validação. Correções identificadas em R7 voltam à etapa correspondente.

## Mapa de cobertura

| Fluxo | Layouts / superfícies |
| --- | --- |
| Entrada | `activity_onboarding`, `item_onboarding` |
| Grupos | `activity_listar_grupos`, `item_grupo`, `dialog_criar_grupo`, menu de editar/excluir, backup/restauração |
| Evento | `activity_listar_participantes`, `item_participante`, `dialog_add_participante`, `dialog_regras`, `item_regra_checkbox`, `activity_configuracoes_grupo` |
| Sorteios | `activity_revelar_amigo`, `activity_historico_sorteios`, `item_sorteio`, `item_sorteio_par`, diálogos de confirmação |
| Compartilhar | `bottom_sheet_compartilhar`, `activity_qrcode` |
| Desejos | `activity_listar_desejos`, `activity_participante_desejos`, `activity_visualizar_desejos`, `item_desejo`, `dialog_add_desejo`, `dialog_opcoes_desejo`, `activity_inserir_desejo`, `activity_alterar_desejo`, `activity_detalhe_desejo` |
| Resumos | `activity_dashboard`, `activity_estatisticas` |
| Estados globais | `empty_state`, `loading_state`, mensagens, diálogos, menus e barras do sistema |

## Validação obrigatória

- `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintRelease` com JDK compatível. Registrar resultado real e caminhos dos relatórios.
- Conferir contraste WCAG AA de texto normal (4,5:1) e cores de estado; não comunicar estado só com cor.
- Alvos de toque de pelo menos 48 dp, descrições para controles sem texto e ícones decorativos fora da leitura do TalkBack.
- Validar fonte ampliada, teclado, rolagem, barras do sistema e ausência de cortes na tela pequena; QR permanece preto sobre branco.
- Percorrer: onboarding → criar grupo → incluir participantes → restrições → sorteio → revelar/ocultar → compartilhar/QR → desejos → histórico → configurações → resumos. Conferir também estados vazios e modo escuro.
- Não disparar mensagens reais ou modificar a publicação na Play Console durante testes.

## Registro das sessões

### S06 — 01/11/2026 — retomada no Claude: revisão visual no aparelho físico

- **Mudança de escopo (ambiente):** o usuário autorizou o aparelho físico `RQ8M605Z1NP` (Samsung SM-A205G, Android 11/API 30, 720 × 1560 px a 280 dpi = 360 × 780 dp, fonte do sistema 1,1) e pediu usá-lo para tudo. Isso substitui a decisão de S02 de validar apenas no emulador; o emulador `Small_Phone` continua disponível, mas não está em uso. Validação de API 30 passa a ser adicional à de API 36 feita em S04.
- Cuidados: o aparelho tem o app de produção `com.amigosecreto.sorteio` com dados do usuário. Testes usam somente o pacote isolado `com.amigosecreto.sorteio.debug` (`pm clear` apenas nesse pacote); nunca tocar no pacote de produção. Idioma do aparelho: inglês, então as capturas S06 estão em inglês. Bateria em ~26% no início; manter o cabo conectado.
- Instalado `app-debug.apk` (SHA-256 `5025…dc04`, igual ao do handoff e mais recente que todos os fontes) sem recompilar. Estado inicial: banco do pacote debug vazio.
- Capturas S06 em `docs/redesign/evidence/v2/device-a205g/` (24 imagens, numeradas na ordem do percurso; inglês, 360 × 780 dp, API 30).
- Percurso feito no aparelho com dados fictícios do pacote `.debug`: onboarding → criar grupo → 3 participantes → sorteio (diálogo SMS/WhatsApp dispensado com “Not now”, nenhuma mensagem enviada) → revelação oculta/visível → QR → desejo → dashboard → configurações → histórico → restrições → folha de compartilhar (sem escolher destino) → estatísticas → modo escuro → fonte 150%. Ao final: fonte do sistema restaurada para 1,1, modo claro, dados do pacote `.debug` limpos (`pm clear`), app de produção intocado.
- A revisão não alterou código. Em seguida, o usuário autorizou corrigir os 7 defeitos (V1, V4, V5, V8, V13, V14, V15); ver “Correções aplicadas” abaixo. As escolhas de design continuam abertas.

**Achados da revisão visual (defeitos corrigidos conforme a seção seguinte; escolhas de design abertas):**

Defeitos (corrigir dentro da direção da revisão 2, sem nova direção visual):

| ID | Tela / evidência | Achado | Etapa |
| --- | --- | --- | --- |
| V5 | Diálogo “New Participant” (`06-…keyboard-buttons-hidden`) | Com o teclado aberto, “Cancel/Add” ficam sob o teclado e o campo de e-mail aparece cortado; só dá para confirmar depois de fechar o teclado | R3 |
| V1 | Toolbar da entrada (`02-toolbar-zoom`, `23-font150-home-subtitle-clipped`) | Logo colado ao título (~1 dp) e alinhado ao topo; título em peso regular e subtítulo em semibold (hierarquia invertida). Com fonte a 150% o subtítulo continua cortado na base no Android 11: a correção de S04 foi verificada só no emulador API 36 | R2 |
| V4 | Diálogos (`04`, `05`, `08`, `16`, `21`) | Três padrões: “New Group” branco com ícone e botão preenchido; “New Participant”, “Draw Complete” e “Restrictions” com a superfície lavanda padrão do Material (cinza neutro no escuro, fora das superfícies azuladas do sistema) e botões só de texto. Restrições usa selo pêssego com X vermelho, fora da paleta | R1 |
| V14 | Telas rolantes (`14-settings-…statusbar-tint`) | Ao rolar, aparece faixa lavanda atrás da barra de status (tint de elevação da AppBar) em vez do fundo plano | R1 |
| V13 | Estatísticas, configurações (`18`, `14`) | A tradução EN usa “$” para valores em reais (`estatisticas_valor_format`, `share_msg_price_*`, hints de orçamento), enquanto desejos mostram “R$”. Em PT é consistente | R6 |
| V8 | Revelação (`09`) | EN “Your Secret Santa is” (`label_your_secret_friend`) está invertido: a pessoa mostrada é quem a participante presenteia (histórico: Ana → Caio) | R4 |
| V15 | Entrada vs. histórico (`03`, `15`) | Datas em formatos diferentes: “01/11/2026” (dia/mês) no card do grupo e “11/01/2026 at 04:54 PM” (mês/dia, 12 h) no histórico | R4 |

**Correções aplicadas (V1, V4, V5, V8, V13, V14, V15) — feitas e verificadas no aparelho, ainda sem commit:**

| ID | Causa encontrada | Correção | Evidência |
| --- | --- | --- | --- |
| V5 | Janela do diálogo sem `adjustResize`: o Android a deslocava para cima e os botões ficavam sob o teclado | `ThemeOverlay.Amigo.MaterialAlertDialog` com `android:windowSoftInputMode=adjustResize`, ligado por `materialAlertDialogTheme`; vale para todos os `MaterialAlertDialogBuilder` | `after-04` |
| V4 | A lavanda era o overlay de elevação do Material 1.13 (branco + ~11% do primário = `#EDEAF9`, medido nas capturas) somado aos tokens `surfaceContainer*` padrão | Tema com `elevationOverlayEnabled=false` e `colorSurfaceContainerLowest/Low/Container/High` → `surface`, `Highest` → `surface_soft`. Diálogos, menus e folha de compartilhar passam a usar as superfícies do sistema (branco no claro, azul-marinho no escuro; pixel medido 255,255,255) | `after-03`, `after-09`, `after-10`, `after-11` |
| V14 | `AppBarLayout` padrão do Material com tint de elevação ao rolar | `Widget.Amigo.AppBarLayout` (`liftOnScroll=false`, fundo `background`, `statusBarForeground` nulo) via `appBarLayoutStyle`; pixel da barra de status = fundo (243,245,250) | `after-06` |
| V1 | `titleMarginStart` não surte efeito ao lado do logo; subtítulo usava a aparência padrão do Material (semibold); o corte a 150% persistia no Android 11 | Logo envolvido em `ic_brand_gift_toolbar.xml` (`<inset>` de 12 dp à direita); `TextAppearance.Amigo.Subtitle` (13 sp regular, `text_secondary`) em `Widget.Amigo.Toolbar`; `titleMarginTop/Bottom` na toolbar da entrada. A 150% o subtítulo aparece inteiro | `after-01`, `after-02` |
| V13 | Traduções EN e ES usavam “$” para valores em reais | “R$” em `configuracoes_hint_valor_*`, `estatisticas_valor_format` e `share_msg_price_*` (EN e ES). Hint conferido no aparelho; o formato das estatísticas só foi conferido pela string (a captura tinha 0 desejos) | `after-06` |
| V8 | EN “Your Secret Santa is” descrevia o contrário do que a tela mostra | EN: “You are the Secret Santa of” (`label_your_secret_friend`, `share_msg_amigo_label`) e `qr_dica` com “reveal who they are gifting” | `after-07` |
| V15 | EN usava `MM/dd/yyyy hh:mm a`; o resto do app (data de criação e evento gravados) é `dd/MM/yyyy` | EN `date_format_display` = `dd/MM/yyyy 'at' HH:mm`; histórico “01/11/2026 at 17:15”. Troca deliberada pela consistência; usuários EN que preferem mês/dia precisariam de datas localizadas também no card (não feito) | `after-08` |

Arquivos alterados nesta rodada: `values/styles.xml`, `values/design_system.xml`, `layout/activity_listar_grupos.xml`, `drawable/ic_brand_gift_toolbar.xml` (novo), `values-en/strings.xml`, `values-es/strings.xml`. Nenhum código Kotlin foi alterado. O tema também uniformizou os cards, que antes recebiam o mesmo tint e agora são brancos (claro) ou azul-marinho (escuro); conferidos participantes, dashboard e configurações.

Verificação desta rodada: `assembleDebug`, 625 unitários / 49 suítes sem falhas, `lintRelease` com 0 erros, 161 avisos e 1 indicação (igual ao baseline). **Atualização — teste tornado robusto:** `GruposActivityTest.setUp` agora desliga o `itemAnimator` do `rv_grupos` (somente no teste; a animação de produção não mudou). Com isso a suíte completa passou **41 de 41, sem falhas/erros/ignorados, em duas execuções no A205G / Android 11** (a segunda, 128,5 s, está em `device-a205g/instrumented-a205g-s06.xml`; uma terceira tentativa interveniente falhou na instalação do APK de teste por `INSTALL_FAILED_VERIFICATION_FAILURE` do aparelho, sem rodar testes, e foi repetida). Isso confirma a hipótese da animação do RecyclerView, que antes só era suspeita. Limite: o teste agora não exercita a animação de troca; nenhum cenário verifica essa animação. O texto seguinte descreve o estado anterior à correção.

Suíte instrumentada **no A205G / Android 11** antes da correção do teste: 40 de 41 aprovados, em duas execuções completas. Falha reproduzível: `GruposActivityTest.editar_nome_grupo_atualiza_lista` (após renomear, a view “Nome Antigo” ainda aparece na hierarquia). O mesmo teste passou 3 de 3 vezes isolado. Não consegui provar que a falha é independente das mudanças: reverti temporariamente só o tema e rodei a suíte de novo, mas essa execução saiu degradada (mais de 10 min, 11 falhas, inclusive testes que haviam passado), então é inconclusiva; os arquivos foram restaurados e conferidos idênticos ao backup. A suspeita é a animação de troca do RecyclerView num aparelho lento com animações do sistema ligadas (escalas 1,0; o emulador anterior as tinha desligadas); confirmar exige desligar as animações do aparelho, o que não fiz por ser configuração de sistema. Mudança não verificada: nenhuma tela de modo escuro além de entrada, grupo, diálogo e menu foi recapturada.

**Revisão independente antes do commit (subagente `code-reviewer`, somente leitura; pontos conferidos por mim):**

| # | Achado | Verificação | Decisão |
| --- | --- | --- | --- |
| R1 | A revelação (`RevelarAmigoActivity`) mostra “Ver lista de desejos” mesmo com `permitirVerDesejos = false`; a lista de participantes já esconde o botão (`ParticipantesActivity.kt:1023`). A tela só ficou alcançável com o toque no card (no HEAD nada a inicia) | confirmado no código | **corrigido** — `ParticipantesActivity` passa `EXTRA_PERMITIR_VER_DESEJOS` ao abrir a revelação e `RevelarAmigoActivity` mantém o botão GONE quando é falso (padrão `false` se o extra faltar). Testes: `revelacao_nao_oferece_desejos_quando_o_grupo_nao_permite` (falhou antes da correção, passa depois) e `revelacao_oferece_desejos_quando_o_grupo_permite` |
| R2 | `DesejosNavigationTest.lista_global_abre_detalhe…` abre `ListarDesejos` (DAO legado; `onDowngrade` é no-op e o helper regrava `user_version = 10`). Os testes de versão comparam antes/depois, então podem mascarar um downgrade se esse teste rodar antes | confirmado: os testes usam `assertEquals(version, …version)` relativo, sem valor absoluto | **corrigido** — os dois testes de versão agora afirmam `AppDatabase.SCHEMA_VERSION` (13) em valor absoluto, antes e depois da ação; `lista_global_abre_detalhe…` foi **removido**, porque abrir `ListarDesejos` (DAO legado) rebaixa o banco para 10 e derrubaria os testes de versão que rodassem depois. Consequência: `ListarDesejos` e `DetalheDesejoActivity` (código morto, sem rota) ficam sem cobertura instrumentada |
| R3 | Mudanças de navegação/comportamento: toque no card abre a revelação; “Limpar” foi para o menu; status “Pronto p/ compartilhar” virou “Toque para revelar”; revelação não depende de `exigirConfirmacaoCompra` | já documentado em S02–S04 | **precisa de aprovação explícita do usuário e menção no commit** |
| R4 | Se o amigo sorteado não existe mais, o card da revelação fica desabilitado sem feedback (antes mostrava “Ninguém”) | do revisor, não reproduzido | aberto, baixa |
| R5 | `SwitchMaterial` no escuro com `elevationOverlayEnabled=false` poderia sumir | **refutado** em captura no A205G: thumb claro sobre trilho escuro, bem visível | fechado |
| R6 | Recursos órfãos: `card_gradient_{orange,blue,green}` (drawable e drawable-night) e a string `status_ready_share`; 0 referências | confirmado por grep | aberto, limpeza opcional |
| R7 | Sem testes para `tv_grupo_context`, `v2_no_people` e o diff com `dataEvento`/`data` em `GruposRecyclerAdapterTest`; contraste de profundidade no escuro (1,17:1 fundo/superfície, 1,47:1 borda); comentário “TODO Phase 2” de moeda obsoleto em `strings.xml`; `insetRight` do logo não inverte em RTL | do revisor | aberto, baixa |

**Verificação das correções R1/R2:** `assembleDebug`, 625 unitários / 49 suítes sem falhas, `lintRelease` com 0 erros, 161 avisos e 1 indicação (igual ao baseline). Suíte instrumentada no A205G / Android 11: **42 de 42** (41 − 1 removido + 2 novos), sem falhas/erros/ignorados, 146,8 s; relatório em `device-a205g/instrumented-a205g-r1r2.xml`. Os 42 substituem a contagem de 41 cenários citada em S04 e acima. R3 (aprovação das mudanças de navegação) segue pendente do usuário; R4, R6 e R7 seguem abertos.

Conferido sem problemas pelo revisor: chaves e placeholders idênticos em PT/EN/ES (também verificado por mim: 0 faltando, 0 divergentes); o formato antigo de data só era exibição (`SorteiosAdapter`) e nenhum teste dependia dele; nenhum DAO legado em tela alcançável nem mudança de schema/ID; nenhum texto novo hardcoded. Dos arquivos `.idea/` (4), nenhum deve entrar no commit. Sem segredos nos arquivos alterados.

Escolhas de design (precisam da sua decisão antes de eu mexer):

| ID | Tela / evidência | Achado | Etapa |
| --- | --- | --- | --- |
| V2 | Cards de grupo (`03`) | Emoji arbitrário por `id % 10` (`GruposActivity.kt:62`; “Family” recebe 🏝️) e ✅ colorido no status “1 of 3 sent”: emojis fora da paleta e sem relação com o grupo. Proposta: avatar com inicial, como nos participantes | R2 |
| V9 | QR (`10`) | Tela mais crua do conjunto: sem cabeçalho composto, título em peso regular, seta de voltar mais clara e metade inferior vazia | R4 |
| V16 | Histórico (`15`) | Só um card com data e linhas “A → B” em texto simples, sem cabeçalho nem tratamento dos pares | R4 |
| V18 | Onboarding (`24`) | Selo pequeno alinhado à esquerda, ~25% vazio no topo, sem ilustração (a arte `bg_gift_scene` existe e não é usada) e sem “Skip” | R2 |
| V10 | Desejos vazio (`11`) | Estado vazio com ícone cinza grande, diferente do selo colorido usado na entrada e no grupo | R5 |
| V12 | Dashboard (`13`) | Ícones de relógio-histórico para data e de loja para local reaproveitados sem relação com o conteúdo | R6 |
| V6 | Participantes (`07`) | Quatro a seis ícones de ação sem rótulo; o de restrições (lista com “+”) não é autoexplicativo | R3 |
| V3 | Grupo vazio | Campo de busca e botão “Draw” com aparência ativa mesmo sem participantes | R3 |
| V11 | Formulário de desejo | Ícone do campo “stores” fica na base e o hint no topo | R5 |

Observação funcional (fora do escopo visual, não alterar neste redesign): abrir a folha de compartilhar já marca o participante como “✓ Result sent” (`ParticipantesViewModel.prepararMensagemCompartilhamento`, linha 383), antes de qualquer destino ser escolhido; é a limitação conhecida do `CLAUDE.md`, que ocorre um passo antes do descrito.

Pontos que ficaram bons: revelação oculta/visível, lista de participantes (claro e escuro), cards de desejo com preço em chip, dashboard/estatísticas em grade, hero da entrada e modo escuro da entrada e do grupo.

Próximo passo: o usuário decide as escolhas de design (V2, V9, V16, V18, V10, V12, V6, V3, V11); cada uma reabre a etapa indicada, roda build/testes/lint e recaptura no aparelho. Não repetido nesta sessão: percurso com restrições salvas, edição de participante, “About”, backup/restauração e telas legadas de desejos. O aparelho ficou com o APK final (`.debug`), fonte 1,1 e modo claro; o pacote `.debug` tem dados fictícios de teste.

### S05 — 01/11/2026 — passagem para Claude

- Usuário pediu continuar no aplicativo Claude desta máquina. Criado `docs/CLAUDE_HANDOFF.md`, com pedido original, feedback, estado da revisão 2, alterações locais, fotos, relatórios, ambiente e próximos passos.
- Adicionada referência ao handoff no início de `CLAUDE.md`. Conferidos branch e `git status`: trabalho permanece sem commit na `feat/redesign-experience`.
- Para retomar o estado local completo, usar a mesma pasta e sessão local sem worktree novo. Não há sincronização de conversa configurada por esta preparação; o contexto necessário está nos documentos do repositório.
- Nenhum código do aplicativo alterado, teste repetido ou publicação realizada nesta sessão. R0–R7 permanecem `done`; a próxima ação é revisar a direção atual com o usuário no Claude e registrar qualquer novo ajuste antes de implementá-lo.

### S04 — 31/10/2026 — segunda direção visual

- Feedback do usuário: a primeira entrega simplificou demais o produto; solicitado acabamento profissional. Reabertas R1–R7 antes de editar a interface.
- Ordem: R1 (paleta, profundidade, tipografia e arte vetorial) → R2/R3 (entrada, cards e evento) → R4/R5/R6 (revelação, desejos e resumos) → R7 (build, testes e fotos reais claro/escuro/fonte ampliada). Os mesmos IDs e dados permanecem preservados.
- Critério visual adicional: composição com contraste entre áreas de destaque e conteúdo, metadados úteis, densidade adequada e ilustrações integradas; não encerrar com mera troca de cor. Não usar números, gráficos ou ações fictícios.
- Evidências da primeira direção mantidas como histórico; novas capturas em `docs/redesign/evidence/v2/`. Revisar `verification.json` como resultado da primeira direção até a validação da segunda.
- Implementados: paleta azul/violeta/dourado; arte vetorial de presentes e marca própria; entrada com composição ilustrada, resumo real e cabeçalho compacto após criar grupos; cards com data/contexto e tratamento de ausência de participantes; evento com painel de informações e dock de ações; avatares circulares e faixas de ações; preço em chip nos desejos; convite ilustrado na revelação; métricas em grade. Temas e tipografia aplicados aos 30 layouts.
- Percurso com dados fictícios: 2 grupos, 3 participantes, 1 sorteio e desejo de R$ 80–150. Estatísticas refletiram média de R$ 115. Cadastro de desejo, sorteio sem envio, revelação/ocultação, QR, prévia de compartilhamento, listas, dashboard e modo escuro conferidos. Capturas intermediárias preservadas em `v2/`.
- Inspeção a 150% encontrou subtítulo da toolbar inicial cortado. A barra passa a `wrap_content` com altura mínima, preservando título e subtítulo; recompilar e recapturar antes de encerrar R7. Indicador de desejos nas estatísticas igualado aos demais contadores; grupos sem pessoas agora exibem “Sem participantes”, e o rodapé usa a data real de criação ou do evento.
- Contraste desta revisão em `v2/contrast.json`: 30 pares semânticos, todos ≥ 4,5:1 (menor razão 5,02:1). Dois checks conjuntos intermediários passaram; falta validar o APK com o último ajuste de fonte e repetir os 41 cenários instrumentados.
- As capturas do teste de desejos detectaram texto da primeira paleta sobre o novo fundo escuro no detalhe legado. R5 permanece aberta até corrigir produto/categoria para `hero_text`/`hero_muted`, recapturar e repetir o teste desse fluxo. Auditados os demais consumidores de `bg_hero`; o ícone do diálogo de grupos também passa a dourado sobre o fundo escuro.
- Encerramento S04: compilação final, 625 testes unitários e lint aprovados após corrigir o contraste (46s). Suíte completa de 41 cenários aprovada (4m53s), seguida de reteste dos três cenários de desejos no APK com a correção final (28s), todos sem falhas/erros/ignorados. Relatórios preservados separadamente em `v2/instrumented-full.xml` e `v2/instrumented-wishes-final.xml`; composição e contraste do detalhe corrigido conferidos na nova captura.
- Evidência consolidada em `v2/verification.json`; índice atual em `evidence/verification.json`. A primeira direção continua em `verification-v1.json`. R1–R7 encerradas como `done` após inspeção e validação desta revisão; aprovação visual do usuário não é presumida. APK final instalado no emulador, em português, com a nova apresentação aberta.

### S01 — 31/10/2026

- Confirmados repositório e identidade na Play Console. Inspecionados manifesto, Gradle, temas, cores e layouts principais.
- Criado este plano antes de qualquer mudança na interface.
- Alterações locais preexistentes em `.idea/` pertencem ao usuário e devem ser preservadas.
- A primeira tentativa de acessar ADB foi interrompida durante a reinicialização do Codex; nova execução autorizada iniciou ADB. Nenhum dispositivo conectado; existe AVD `Small_Phone`.
- Próximo passo: capturar a interface atual no AVD, concluir R0 e implementar R1 em seguida.
- Atualização: build debug da versão anterior passou com JDK 21. O AVD iniciou, mas sua janela não é exposta pela ferramenta de controle visual (não aparece no inventário de aplicativos; seleção por `qemu-system-x86_64` recusada). Capturas ficam pendentes. Para não bloquear a implementação dos recursos, R1–R6 podem avançar com inspeção de XML, compilação e verificações existentes; a revisão visual continua obrigatória antes de `done` e da entrega final. Esta é a única alteração de ordem.

### S02 — 31/10/2026 — retomada após reinicializações

- Confirmado o estado do trabalho antes de retomar; preservadas as mudanças locais de `.idea/` (a IDE também atualizou `misc.xml`).
- Implementado o sistema visual nos 30 layouts, com temas claro/escuro unificados. Acrescentados menu explícito nos grupos, controles de 48 dp, rolagem na revelação/onboarding e formulários em diálogo.
- Corrigidos os indicadores esticados do onboarding e os títulos antes ocultados por código na tela inicial e no QR. O cabeçalho dos desejos mostra o nome sem emoji adicional.
- Última validação conjunta após esses ajustes: build debug, 625 testes unitários e lint passaram (`BUILD SUCCESSFUL in 2m 59s`). Evidência consolidada em `docs/redesign/evidence/verification.json`.
- O SDK/emulador da máquina foi atualizado durante a sessão (emulador 36.5 → 37.2). Reiniciada a conexão ADB e iniciada instância de teste sem janela; nenhum dado de produção foi acessado ou alterado.
- Diagnósticos do ambiente preservados em `00-emulator-system-ui-anr.png` e `00-emulator-blank.png`; **não são capturas de antes/depois do aplicativo**. Após recuperar a sessão e reinstalar o APK, o onboarding renderizou corretamente em português.
- Conferida a captura `02-after-onboarding.png`: títulos, corpo, CTA, indicadores e barras do sistema visíveis na tela de 360 × 640 dp. Demais páginas e fonte ampliada ainda pendentes de percurso.
- Usuário confirmou continuar apenas com o emulador. Iniciada a suíte existente de 35 testes instrumentados; aguardar o resultado antes de interagir no dispositivo.
- A primeira execução instrumentada encontrou título do estado vazio fora da área visível após excluir grupo. A captura `03-instrumented-run.png` confirmou que o hero da entrada está alto demais para 360 × 640 dp. R2 deve compactar esse bloco antes de concluir.
- Outra falha ocorreu ao criar dois grupos em sequência: o diálogo anterior continuava aberto no momento do segundo toque. Conferir fechamento/teclado sem animações no ambiente de teste, sem enfraquecer as asserções.
- A captura `04-instrumented-participants.png` mostrou título duplicado e excesso de decoração no formulário. R1 reaberta para usar `MaterialAlertDialogBuilder` nos diálogos; R3 deve simplificar o formulário e adaptar as ações à largura disponível. A revisão visual está cumprindo seu papel: estas etapas não podem ser marcadas como `done` ainda.
- Primeira rodada instrumentada encerrada: 35 cenários, 33 aprovados e os dois problemas de grupos acima. Aplicados hero compacto, formulário sem cabeçalho duplicado, `MaterialAlertDialogBuilder`, identificação correta de edição de grupo e ações em 6 colunas a partir de 360 dp / 3 abaixo disso. Build, 625 unitários e lint passaram novamente em 2m37s.
- Capturas anteriores finalmente registradas em português: `05-before-onboarding.png` e `06-before-home.png`. Conferidas a entrada compacta (`07-after-home.png`) e a criação com teclado (`08-create-group.png`): grupo salvo e refletido na lista.
- O percurso de R3 encontrou um problema anterior ao redesign: `ParticipantesActivity` configurava `supportActionBar` sem registrar sua toolbar. A captura `09-participants-empty.png` confirma ausência de voltar, histórico e menu, impedindo acessar configurações/resumo pelo fluxo normal. Corrigir esse vínculo e acrescentar teste de navegação real para configurações; o total de cenários passa a 36. Compactar também o estado vazio que encosta no painel inferior.
- R5: criação, leitura, edição e exclusão de desejo conferidas no emulador com dados fictícios; faixa R$ 50–100 preservada na edição. A lista global e seu detalhe são telas legadas sem rota a partir da entrada atual. Permanecem no escopo visual e recebem um teste de compatibilidade lista → detalhe → retorno, com desejo ligado a um participante e verificação da ação de busca acessível por rolagem. Total final previsto: 37 testes instrumentados. Capturas opcionais desse cenário são habilitadas com `redesignScreenshots=true`.
- Com três participantes cadastrados, a captura `15-participants-navigation.png` confirmou o menu recuperado, mas também o limite anterior de uma lista com altura de um item dentro de outra rolagem. R3 passa a usar uma lista com viewport próprio, busca no cabeçalho e painel de ações fora do conteúdo. A ação “Limpar participantes” vai para o menu com a mesma confirmação e operação; acrescentar teste de limpeza pelo novo caminho (38 cenários no total). Preservar os IDs usados na navegação e verificar busca com teclado aberto/fechado.
- A lista corrigida e a busca com teclado foram conferidas em `16-participants-scroll.png` e `17-search-keyboard.png`. Sorteio com Ana, Bia e Caio e uma restrição de Ana foi concluído (`19-draw-result.png`), sem enviar mensagens. O percurso de R4 revelou que `RevelarAmigoActivity` também estava sem ponto de entrada no código atual. Vincular o card ao resultado apenas após existir sorteio, reutilizar o texto “Toque para revelar” e acrescentar regressão de ocultar → revelar → ocultar (39 testes). A captura inicialmente chamada `20-reveal-hidden.png` mostrou a ausência de navegação e não é evidência da revelação pronta.

### S03 — 31/10/2026 — revisão final no emulador

- Mantida a escolha do usuário de validar apenas no emulador. Reinstalado o APK atual preservando os dados fictícios.
- Conferidos os números das estatísticas: 1 grupo, 3 participantes, 1 sorteio, 1 desejo e média de R$ 75,00. Dashboard e histórico também conferidos.
- Corrigida duplicação do símbolo monetário no cabeçalho e nos textos de desejos, sem modificar valores armazenados. Build, 625 testes unitários e lint passaram em 1m06s após as correções de navegação, restrições e moeda.
- Revelação efetivamente acessada pelo card: Ana → Caio; ocultar remove o resultado; Bia → Ana abre a lista somente de leitura com Caneca. QR preto sobre branco e folha de compartilhamento conferidos sem envio externo.
- Capturas `25`–`36` registram estatísticas, revelação, desejos, QR, compartilhamento, modo escuro e fonte a 150%. A barra de ações dos diálogos Material pode rolar quando os botões se empilham com fonte ampliada; conferir acesso ao cancelamento antes de concluir R7.
- Iniciada nova suíte de 39 cenários instrumentados, incluindo as regressões de configurações, limpeza pelo menu, revelação e compatibilidade da lista global de desejos. Não declarar aprovação antes do resultado real.
- Revisão de `29-revealed-wishes.png`: o nome não estava vinculado ao cabeçalho e as duas listas por participante ainda usavam ListView dentro de rolagem externa. R5 volta a correção: viewport independente e nome preenchido. A tela somente de leitura ainda abria DAO legado, incompatível com a versão atual do Room conforme `CLAUDE.md`; migrar sua leitura ao repositório atual para preservar o banco no caminho de revelação recém-recuperado. Acrescentar regressão com múltiplos desejos e reabertura do banco.
- A revelação usa os mesmos DAOs legados: migradas as consultas de amigo/desejos para os repositórios Room, sem mudar schema. A regressão de revelação verifica que a versão do banco é preservada; dois cenários adicionais verificam rolagem até o nono desejo nas listas editável e somente de leitura. Total final: 41 cenários. A execução de 39 já iniciada valida o APK anterior a este último refinamento; repetir a cobertura afetada após compilar.
- Rodada de 39 encerrada: 38 aprovados, uma falha na asserção de ausência de view após limpar participantes. ListView mantém filhos reciclados mesmo oculto, como já documentado na suíte de grupos. O teste agora exige estado vazio visível, lista oculta, adapter com zero itens e banco com zero participantes; não basta ocultar visualmente os dados. Relatório desta rodada preservado em `docs/redesign/evidence/instrumented-round2.xml`. Repetir os 41 cenários no APK final.
- Build final compilou. A guarda arquitetural unitária exigiu retirar `RevelarAmigoActivity` e `VisualizarDesejosActivity` das exceções de DAOs legados após a migração; lista reduzida de quatro para duas telas e `CLAUDE.md` atualizado. Check conjunto repetido após essa atualização.
- Conferidas as cinco páginas de onboarding (`38-onboarding-2.png` a `38-onboarding-5.png`), incluindo última página com fonte 150% (`39-onboarding-large-font.png`). Ações do diálogo Material realmente rolam: cancelamento acessado e diálogo fechado com fonte 150% (`40-dialog-actions-large-font.png`). Fonte restaurada a 100% e modo claro ao finalizar a inspeção.
- Recontagem final do manifesto e diretório de layouts corrigiu o inventário inicial: 15 Activities declaradas e 30 layouts (a contagem preliminar era 16/32). Todos os 30 layouts estão no mapa e receberam os estilos. Contraste consolidado em `contrast.json`: 22 pares claro/escuro, menor razão 5,60:1.
- Encerramento: check conjunto passou em 1m17s, com 625 unitários aprovados; suíte final completa passou em 6m01s, com 41 cenários, zero falhas, erros ou ignorados. Listas longas conferidas nas capturas `41-*`. Relatório final preservado em `instrumented-final.xml` e resultado consolidado em `verification.json`. R0–R7 encerradas como `done` após conferência das evidências. APK final reinstalado no emulador para revisão.

## Entrega atual — revisão 2

- **Segunda direção implementada e validada localmente. R0–R7: done.** Nova composição azul/violeta/dourado, arte vetorial própria, cabeçalhos compostos, cards com metadados e faixa de ações, preço em chip, revelação ilustrada e indicadores em grade. Sistema vigente: `docs/REDESIGN_DESIGN_SYSTEM.md`. Branch: `feat/redesign-experience`.
- **Validação:** build debug aprovado; 625 unitários / 49 suítes, zero falhas/erros/ignorados. Suíte completa de 41 cenários aprovada e três cenários de desejos repetidos após o último ajuste de contraste, também aprovados. Não apresentar os dois relatórios como 44 cenários únicos. Lint final: zero erros/fatais, 161 avisos e 1 indicação informativa. Contraste: 30 pares, menor razão 5,02:1.
- **Inspeção real:** emulador Android 16/API 36, 360 × 640 dp, modo claro/escuro e fonte 100%/150%. Criados grupos/participantes e desejo fictícios, realizado sorteio sem envio e conferidos revelar/ocultar, QR, prévia de compartilhamento, listas, formulários, data/orçamento do evento, histórico, restrições, dashboard e estatísticas. Listas longas, detalhe e onboarding percorridos também pelos testes.
- **Fotos atuais:** `docs/redesign/evidence/v2/README.md`. Priorizar `17-home-final.png`, `20-event-final.png`, `08-reveal-visible.png`, `23-statistics-final.png`, `18-home-large-font-final.png` e `test-wish-detail.png`. Imagens intermediárias estão identificadas. `24-onboarding-final.png` registra a reinstalação final em português.
- **Resultados verificáveis:** `docs/redesign/evidence/v2/verification.json`, `contrast.json`, `instrumented-full.xml` e `instrumented-wishes-final.xml`. Relatórios de unitários/lint em `app/build/reports/tests/testDebugUnitTest/index.html` e `app/build/reports/lint-results-release.html`.
- **APK:** `app/build/outputs/apk/debug/app-debug.apk`, pacote isolado `com.amigosecreto.sorteio.debug`. SHA-256 `502570685c0f4f3a92db23f7c0347e86c74c56c4334804268ecd018d80b9dc04`. A revisão 1 foi preservada temporariamente em `/private/tmp/amigosecreto-redesign-v1.apk` e `/private/tmp/amigosecreto-redesign-v1-source.zip`.
- **Limites:** execução validada apenas no emulador/API 36, por escolha do usuário. Nenhuma mensagem externa enviada, alteração de schema ou publicação realizada. Avisos de lint permanecem documentados; não equivalem a erros bloqueantes. Alterações preexistentes da IDE preservadas. Sem commit/push/PR.
- **Próxima sessão:** ler este plano e `git status`; registrar feedback e eventuais ajustes antes de editar. A revisão 2 está pronta para avaliação visual do usuário. Preparação e publicação de release na Play Store são trabalhos separados.
