# Revisão 2 — Encontros

Direção solicitada pelo usuário: composição com mais identidade e acabamento profissional. Capturas reais no emulador Small_Phone, Android 16/API 36, 360 × 640 dp; dados fictícios. Plano e andamento em [REDESIGN_PLAN.md](../../../REDESIGN_PLAN.md).

## Capturas principais

| Captura | Evidência |
| --- | --- |
| [17-home-final.png](17-home-final.png) | Marca, arte vetorial, cabeçalho adaptado, resumo real e cards com metadados |
| [18-home-large-font-final.png](18-home-large-font-final.png) | Fonte a 150%; toolbar cresce sem cortar verticalmente o subtítulo, conteúdo rolável |
| [20-event-final.png](20-event-final.png) | Cabeçalho com contagem, data e orçamento; cards e painel inferior compacto |
| [12-participants-dark.png](12-participants-dark.png) | Modo escuro, estados do sorteio, seis ações acessíveis e avatares |
| [13-participants-large-font.png](13-participants-large-font.png) | Lista com fonte a 150% |
| [05-wishes.png](05-wishes.png) | Desejo cadastrado, categoria e faixa de R$ 80–150 em chip |
| [07-reveal-hidden.png](07-reveal-hidden.png), [08-reveal-visible.png](08-reveal-visible.png) | Convite ilustrado, ocultar/revelar e resultado em dourado |
| [06-dashboard.png](06-dashboard.png) | Indicadores do grupo organizados em grade |
| [23-statistics-final.png](23-statistics-final.png) | 2 grupos, 3 participantes, 1 sorteio, 1 desejo; média R$ 115 |
| [19-settings.png](19-settings.png) | Cabeçalho temático e formulário do evento |
| [21-history.png](21-history.png), [22-restrictions.png](22-restrictions.png) | Histórico e restrições preservados |
| [15-qr.png](15-qr.png), [16-share-sheet.png](16-share-sheet.png) | QR preto/branco e prévia de compartilhamento; nenhum envio externo |

## Validação e histórico da rodada

`contrast.json`: 30 pares semânticos, menor contraste 5,02:1. [verification.json](verification.json) consolida build, 625 unitários, lint sem erros, a suíte completa de 41 cenários e o reteste dos três cenários de desejos após a correção final de contraste. Os dois relatórios preservados distinguem as execuções; são 41 cenários únicos.

As capturas `01`, `02`, `03`, `04`, `09`, `10`, `11` e `14` documentam o desenvolvimento desta rodada. Em especial, `09`/`11` ainda têm o texto “0 Participante” e rodapé genérico, corrigidos depois; `10` tem um contador menor, corrigido em `23`; `14` mostra o corte vertical do subtítulo antes da toolbar adaptativa, conferida em `18`. Não usar essas versões intermediárias como material de publicação.

As capturas automatizadas `test-*.png` cobrem listas longas e detalhe legado. A primeira captura do detalhe detectou texto escuro sobre o novo cabeçalho escuro e foi preservada com sufixo `before-contrast`. As imagens sem esse sufixo foram recapturadas após a correção e o reteste. Capturas automáticas podem usar o idioma do sistema de teste; as capturas manuais principais estão em português.


O APK final foi reinstalado em português no emulador; [24-onboarding-final.png](24-onboarding-final.png) registra sua apresentação. Nenhuma publicação na Play Store ou envio de mensagens externas foi realizado.
