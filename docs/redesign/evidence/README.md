# Evidências do redesign

**Direção vigente: [revisão 2](v2/README.md)**, criada após o feedback do usuário de que a primeira proposta estava simplificada demais. As capturas listadas abaixo registram a primeira direção e seus refinamentos técnicos; não representam a aprovação visual da revisão atual. `verification-v1.json` preserva a validação anterior, enquanto `verification.json` acompanha a versão atual.

O status de entrega fica em [REDESIGN_PLAN.md](../../REDESIGN_PLAN.md). As capturas foram feitas no emulador `Small_Phone`, Android 16/API 36, tela de 720 × 1280 px (360 × 640 dp), usando dados de teste.

| Arquivo | O que documenta |
| --- | --- |
| `verification.json` | Resultado consolidado dos checks; consultar o campo de revisão visual separadamente |
| `contrast.json` | 22 combinações semânticas claro/escuro, incluindo os dois extremos do gradiente de revelação; todas acima de 4,5:1 |
| `00-emulator-system-ui-anr.png` | Travamento do System UI durante a recuperação do emulador, com a versão anterior instalada; diagnóstico do ambiente |
| `00-emulator-blank.png` | Tela preta durante a recuperação; não representa uma tela entregue do aplicativo |
| `02-after-onboarding.png` | Onboarding em português com indicadores corrigidos e conteúdo visível |
| `03-instrumented-run.png` | Primeira composição da entrada; revelou excesso de altura no hero e precisa do refinamento registrado no plano |
| `04-instrumented-participants.png` | Primeira composição do formulário; revelou título duplicado e decoração excessiva e precisa do refinamento registrado no plano |
| `05-before-onboarding.png` | Onboarding anterior em português, antes dos novos estilos e textos |
| `06-before-home.png` | Entrada anterior em português |
| `07-after-home.png` | Entrada compacta após corrigir a altura do hero |
| `08-create-group.png` | Diálogo de criação; preenchimento com teclado e salvamento conferidos |
| `09-participants-empty.png` | Captura intermediária que revelou toolbar não vinculada e estado vazio próximo ao painel |
| `10-add-participant.png` | Formulário simplificado, antes do refinamento final dos rótulos |
| `11-participants-list.png` | Card com ações de 48 dp; captura anterior ao vínculo da toolbar |
| `12-wishes-empty.png` | Lista de desejos vazia; captura anterior à atualização do texto de apoio |
| `13-add-wish.png` | Formulário de desejo; revelou ícones antigos que foram substituídos |
| `14-wish-options.png` | Opções de edição/remoção com a nova linguagem visual |
| `15-participants-navigation.png` | Menu recuperado; captura anterior à correção da rolagem aninhada |
| `16-participants-scroll.png` | Lista de participantes com viewport independente |
| `17-search-keyboard.png` | Busca com teclado aberto e painel inferior recolhido |
| `18-restrictions.png` | Restrições; captura anterior ao ajuste da nota para dentro da rolagem |
| `19-draw-result.png` | Sorteio concluído com dados fictícios, sem enviar mensagens |
| `20-card-missing-route.png` | Diagnóstico da rota de revelação ausente; não representa a revelação pronta |
| `21-group-settings.png`, `22-group-settings-budget.png` | Configurações do evento, orçamento e preferências |
| `23-dashboard.png`, `24-draw-history.png` | Resumo e histórico com os dados conferidos |
| `25-statistics.png`, `26-statistics-bottom.png` | Estatísticas: 1 grupo, 3 participantes, 1 sorteio, 1 desejo e média R$ 75 |
| `27-reveal-hidden.png`, `28-reveal-visible.png` | Revelação realmente acessada pelo card, antes e depois do toque |
| `29-revealed-wishes.png` | Lista somente de leitura; revelou cabeçalho sem nome e necessidade de viewport independente |
| `30-qr-code.png` | QR em preto/branco com ação de salvar acessível |
| `31-share-sheet.png` | Folha de compartilhamento; nenhum destino externo acionado |
| `32-participants-dark.png`, `33-reveal-dark.png` | Modo escuro |
| `34-participants-large-font.png`, `35-reveal-large-font.png` | Fonte 150% no modo escuro; conteúdo e ações com rolagem |
| `36-form-large-font-dark.png` | Diálogo com fonte 150%; ações Material empilhadas em área rolável |
| `37-global-wishes.png`, `37-wish-detail.png`, `37-wish-detail-bottom.png` | Lista global legada e detalhe, capturados pelo teste instrumentado |
| `38-onboarding-2.png` a `38-onboarding-5.png` | Demais páginas da apresentação, conteúdo e CTA conferidos |
| `39-onboarding-large-font.png` | Última página com fonte a 150%; texto rolável e CTA preservado |
| `40-dialog-actions-large-font.png` | Área de ações do diálogo rolada, exibindo o cancelamento que foi acionado com sucesso |
| `41-editable-wishes-multiple.png`, `41-revealed-wishes-multiple.png` | Listas finais com nove desejos; rolagem até o último item e nome do participante corrigido |
| `42-final-onboarding.png`, `43-final-home.png` | APK final reinstalado, onboarding concluído e entrada conferida; aplicativo deixado aberto no emulador |
| `instrumented-round2.xml` | Rodada intermediária: 38/39 aprovados; asserção sobre view reciclada posteriormente substituída por verificações de UI, adapter e banco |
| `instrumented-final.xml` | Suíte final completa: 41 cenários, zero falhas, erros ou ignorados |

Não usar capturas intermediárias como material da Play Store. Não houve publicação ou envio de mensagens reais durante estes testes.
