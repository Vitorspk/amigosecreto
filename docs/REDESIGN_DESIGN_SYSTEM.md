# Sistema visual — Encontros / revisão 2

Referência vigente do [plano de redesign](REDESIGN_PLAN.md), atualizada após o feedback do usuário em 31/10/2026. A primeira direção simplificou demais o produto; esta revisão substitui sua composição e seus tokens visuais.

## Identidade

Um organizador de encontros com presença visual, informação bem organizada e um momento especial de revelação. Azul profundo e violeta estruturam a marca; dourado ilumina ilustrações e detalhes de celebração. As áreas de destaque contrastam com superfícies de trabalho mais claras e discretas.

| Papel | Claro | Escuro | Recurso |
| --- | --- | --- | --- |
| Fundo | `#F3F5FA` | `#0E1728` | `background` |
| Superfície | `#FFFFFF` | `#19253A` | `surface` |
| Texto principal | `#19263D` | `#F0F3FA` | `text_primary` |
| Texto de apoio | `#5E6A7D` | `#B8C4D8` | `text_secondary` |
| Ação | `#5D42CF` | `#C9B9FF` | `md_theme_primary` |
| Sobre a ação | `#FFFFFF` | `#35205D` | `md_theme_onPrimary` |
| Destaque escuro | `#172940` → `#3E2E76` | igual | `reveal_start` / `reveal_end` |
| Texto do destaque | `#FFFFFF` | igual | `hero_text` |
| Apoio do destaque | `#D4DDF1` | igual | `hero_muted` |
| Dourado | `#F4C477` | igual | `brand_gold` |

As cores ficam em `values/colors.xml` e `values-night/colors.xml`. Não usar branco fixo sobre `primary`: a ação é clara no modo escuro. O branco fixo é apropriado ao destaque escuro e ao fundo do QR. O dourado serve como texto apenas nos fundos escuros conferidos pelo relatório de contraste.

## Composição e hierarquia

- Margens principais de 20 dp; cards com 16–20 dp internos, cantos de 18 dp e elevação discreta de 1 dp. A faixa de ações usa uma superfície secundária; o conteúdo não se mistura aos controles.
- Botões com cantos de 14 dp e alvos de pelo menos 48 dp; manter seletores Material de foco, pressão e desabilitado.
- Títulos de barra de 20 sp, títulos de conteúdo de 20–28 sp, números de 30–32 sp, corpo de 14–16 sp e metadados de 12–13 sp. Rótulos curtos de destaque podem usar 10–11 sp, tracking e contraste alto.
- Entrada: marca vetorial, cabeçalho ilustrado, chamada de ação, resumo real e cards de grupos. O texto de apoio do cabeçalho se recolhe quando há grupos, para priorizar o trabalho. A toolbar cresce com fonte ampliada para não cortar seu subtítulo.
- Grupo: cabeçalho escuro com contexto e contagem, busca, lista com rolagem própria e duas ações no painel inferior. O painel reserva espaço no viewport e é recolhido com teclado aberto.
- Participantes: avatar circular, nome, estado, contagem de desejos e faixa de ações com ícones de 48 dp. Cores discretas nos controles; vermelho reservado à remoção.
- Desejos: ícone em badge, produto, categoria e preço em chip semântico. Listas por participante têm viewport próprio, sem ListView dentro de outra rolagem.
- Revelação: superfície de convite com arte vetorial, nome escondido até o toque, resultado em dourado e acesso aos desejos. O conteúdo rola com fonte ampliada.
- Resumos: grade de duas colunas com ícone, rótulo e valor; primeiro indicador escuro e demais em superfícies claras. Datas e local mantêm tamanho de texto apropriado. Não inserir gráficos ou números fictícios.

## Arte e componentes

`bg_gift_scene.xml` é uma ilustração vetorial de presentes com laços, volume, órbitas e detalhes dourados. Não depende de bitmap nem de rede. `ic_brand_gift.xml` identifica o produto na entrada; `ic_group.xml` distingue a métrica de grupos.

`values/design_system.xml` centraliza tipografia, widgets e dimensões. `Base.Theme.AmigoSecreto` mantém Material 3 DayNight. Fundos semânticos: `bg_hero`, `bg_reveal`, `bg_avatar`, `bg_action_strip`, `bg_price_pill`, `bg_icon_soft` e `bg_surface_soft`.

Preservar marcas e cores dos destinos de compartilhamento. QR sempre preto sobre branco. Os nomes antigos de cores permanecem como aliases de compatibilidade; não reintroduzir gradientes antigos nas linhas das listas.

## Acessibilidade, dados e adaptação

Texto e botões crescem com a fonte; formulários, diálogos e revelação rolam. Controles sem texto mantêm descrição, ícones decorativos ficam fora da leitura do TalkBack. Ações dos participantes têm seis colunas a partir de 360 dp e três em telas menores.

Diálogos usam `MaterialAlertDialogBuilder`; botões podem se empilhar e rolar com fonte ampliada. As traduções de apresentação e novos rótulos acompanham PT/EN/ES. Metadados são derivados dos grupos reais, sem novas consultas por linha. Não abrir DAOs legados nas telas alcançáveis.

O acompanhamento e a evidência real de cada etapa ficam no plano. Este documento descreve o sistema vigente; testes antigos não comprovam o acabamento desta revisão.
