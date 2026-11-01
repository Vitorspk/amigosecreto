# Passagem de contexto — Codex → Claude

Preparado em 01/11/2026. Este arquivo resume o trabalho já realizado; o acompanhamento obrigatório permanece em `docs/REDESIGN_PLAN.md`.

## Comece por aqui

1. Trabalhe na pasta existente `/Users/home/Documents/workspace-github/amigosecreto` e execute `pwd`, `git branch --show-current` e `git status --short`.
2. Leia `AGENTS.md`, `CLAUDE.md`, `docs/REDESIGN_PLAN.md` e `docs/REDESIGN_DESIGN_SYSTEM.md`.
3. Confira `docs/redesign/evidence/v2/README.md` e `docs/redesign/evidence/v2/verification.json`; abra as capturas principais.
4. Retome a avaliação visual da revisão 2 com o usuário. Não reinicie o projeto nem invente uma terceira direção sem novo feedback. Não há feature adicional definida para implementação neste handoff.

## Pedido original e feedback

O usuário é o desenvolvedor do aplicativo Android **Amigo Secreto Sorteio QR Code**, pacote de produção `com.amigosecreto.sorteio`. Pediu analisar e modernizar todas as telas, com um plano no diretório `/docs`, atualizado em cada sessão e com `done` apenas para entregas implementadas e validadas.

A primeira direção visual foi implementada e testada, mas o usuário a considerou mais simples que a produção e pediu algo profissional. Ela não é a referência atual. A **revisão 2** usa azul profundo, violeta e dourado, arte vetorial própria de presentes, cabeçalhos compostos, cards com metadados, preço em chip, revelação ilustrada e métricas em grade. O usuário recebeu fotos dessa revisão, mas ainda não deu aprovação visual explícita.

Em 31/10, pediu parar e continuar no dia seguinte. Em 01/11, pediu transferir a continuidade para o aplicativo Claude nesta máquina. A preparação deste documento não alterou código do aplicativo nem repetiu testes.

## Estado do Git — atenção à continuidade local

- Branch conferida em 01/11: `feat/redesign-experience`.
- O redesign está **sem commit, push ou PR**, com arquivos modificados e arquivos novos não rastreados. Não presumir que um clone, checkout limpo ou worktree novo contém esse trabalho.
- Para continuar no Claude Desktop, escolher ambiente local, a mesma pasta e **Worktree desativado**. Conferir os arquivos antes de editar.
- Preservar todas as alterações atuais. Não executar limpeza/reset/checkout que descarte o trabalho.
- Alterações preexistentes do usuário na IDE: `.idea/deploymentTargetSelector.xml`, `.idea/gradle.xml` e `.idea/appInsightsSettings.xml`; Android Studio também modificou `.idea/misc.xml`. Não incluir/reverter esses arquivos automaticamente.
- Não editar simultaneamente pelo Codex e pelo Claude.

## Implementação atual

- Kotlin, Views/XML, Material 3 DayNight, ViewBinding e Room/Hilt; 15 Activities declaradas, 30 layouts, minSdk 24 e compile/target 36.
- Recursos compartilhados em `app/src/main/res/values/design_system.xml`, `styles.xml`, `colors.xml` e `values-night/colors.xml`.
- Arte vetorial em `drawable/bg_gift_scene.xml`; marca em `ic_brand_gift.xml`; fundos semânticos `bg_hero`, `bg_reveal`, `bg_avatar`, `bg_action_strip` e `bg_price_pill`.
- Entrada: cabeçalho ilustrado, resumo derivado dos grupos reais, cards com contexto/data e menu explícito. Texto de apoio do hero se recolhe quando há grupos. Toolbar cresce com fonte ampliada.
- Participantes: toolbar registrada corretamente; lista com viewport próprio, busca e tratamento de teclado; painel inferior compacto. Limpar participantes fica no menu e mantém confirmação.
- Revelação recuperada pelo toque no card após sorteio; resultado inicialmente oculto, com alternância revelar/ocultar. Consultas dessa tela e da lista somente de leitura migradas para os repositórios Room.
- Desejos: CRUD preservado; listas editável e somente de leitura com rolagem independente, inclusive nove itens. Cabeçalho do detalhe usa texto claro sobre fundo escuro.
- Dashboard/estatísticas em grade com números reais. QR preto sobre branco, destinos de compartilhamento preservados, traduções dos novos textos em PT/EN/ES.

## Cuidados técnicos que não podem ser perdidos

- Não usar `MySQLiteOpenHelper`/DAOs legados nas telas alcançáveis: podem rebaixar a versão do banco gerenciado por Room. Ler a seção correspondente em `CLAUDE.md` e preservar `DaosLegadosGuardTest`.
- `ListarDesejos` e `DetalheDesejoActivity` são telas legadas sem entrada pelo fluxo principal. Não adicionar uma rota pública para elas sem resolver seu acesso legado aos dados.
- Não voltar a colocar ListView com altura `wrap_content` dentro de outra rolagem; isso já limitou a lista a um item.
- Não usar `md_theme_onPrimaryContainer` sobre `bg_hero` escuro: usar `hero_text`, `hero_muted` ou `brand_gold`, conforme o sistema visual.
- ListView pode manter views recicladas quando oculto. O teste de limpeza exige estado vazio, adapter sem itens e banco sem participantes, em vez de apenas ausência de um TextView na hierarquia.
- Preservar IDs, navegação, dados, restrições e compartilhamento. Não remover funcionalidades para simplificar o visual.

## Evidência e validação já realizadas em 31/10

- Build debug aprovado e **625 testes unitários / 49 suítes**, sem falhas, erros ou ignorados.
- Suíte completa com **41 cenários instrumentados** aprovada; após corrigir duas cores/ícone, os **três cenários de desejos foram repetidos** e aprovados. São 41 cenários únicos, não 44.
- Lint: zero erros/fatais, 161 avisos e 1 indicação informativa. Não afirmar que o lint ficou sem avisos.
- Contraste calculado em 30 pares, menor razão 5,02:1.
- Inspeção no emulador Small_Phone, Android 16/API 36, 360 × 640 dp, modos claro/escuro, fontes 100%/150%, teclado e rolagem.
- ~~Usuário escolheu continuar apenas com emulador.~~ **Atualizado em 01/11 (S06):** o usuário autorizou o aparelho físico `RQ8M605Z1NP` (Samsung SM-A205G, Android 11/API 30) e pediu usá-lo para tudo. Testar somente o pacote `com.amigosecreto.sorteio.debug`; o app de produção `com.amigosecreto.sorteio` está nesse aparelho com dados reais e não deve ser tocado. Restaurar fonte do sistema (1,1) e modo claro ao terminar. Ver `docs/REDESIGN_PLAN.md`, sessão S06.
- Relatórios persistidos: `docs/redesign/evidence/v2/instrumented-full.xml`, `instrumented-wishes-final.xml`, `contrast.json` e `verification.json`.
- Resultados são históricos dessa execução, não testes novamente executados em 01/11. Revalidar o que for afetado por alterações novas.

Fotos prioritárias em `docs/redesign/evidence/v2/`:

- `17-home-final.png`
- `20-event-final.png`
- `08-reveal-visible.png`
- `23-statistics-final.png`
- `12-participants-dark.png`
- `18-home-large-font-final.png`
- `test-wish-detail.png`

O README das evidências distingue imagens intermediárias, correções e idiomas do ambiente de teste. Capturas sem `v2/` representam principalmente a primeira direção e diagnósticos; não apresentá-las como a proposta atual.

## Ambiente e comandos

Android Studio e SDK/ADB estão instalados. Conferir o estado atual do emulador antes de usar; não presumir que a instância da sessão anterior permaneceu aberta.

```sh
cd /Users/home/Documents/workspace-github/amigosecreto
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintRelease -Dorg.gradle.jvmargs=-Xmx1536m --max-workers=2
```

```sh
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home ./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.redesignScreenshots=true -Dorg.gradle.jvmargs=-Xmx1536m --max-workers=2
```

ADB: `/Users/home/Library/Android/sdk/platform-tools/adb`.
Emulador: `/Users/home/Library/Android/sdk/emulator/emulator`.
AVD validado: `Small_Phone`. A instância anterior usava inicialização sem janela, com `-gpu swiftshader`, memória 1536 MB e 2 cores; o próprio emulador pode elevar a memória mínima.

Usar JDK 21, não depender do Java padrão da máquina. Os limites de memória/workers foram usados para reduzir contenção com o emulador. Permissões do Claude devem ser configuradas no próprio aplicativo; não presumir que permissões concedidas no Codex se transferem.

## APK e publicação

- APK: `app/build/outputs/apk/debug/app-debug.apk`.
- Pacote de teste isolado: `com.amigosecreto.sorteio.debug`.
- SHA-256 validado: `502570685c0f4f3a92db23f7c0347e86c74c56c4334804268ecd018d80b9dc04`.
- Foi reinstalado e aberto em português no emulador ao encerrar a sessão anterior.
- Não houve publicação na Play Store, envio de mensagens externas ou alteração de schema. Publicação é trabalho separado.
- Cópias temporárias da revisão 1 existiam em `/private/tmp/amigosecreto-redesign-v1.apk` e `/private/tmp/amigosecreto-redesign-v1-source.zip`; podem não sobreviver a uma limpeza/reinicialização. O estado atual não depende desses arquivos.

## Como continuar cada sessão

Ler o plano e `git status`; registrar novo feedback/escopo antes de editar; reabrir somente etapas afetadas; executar checks adequados; capturar telas reais e atualizar plano/evidências no fim. O fato de R0–R7 estarem `done` comprova a entrega técnica da revisão 2, não aprovação visual presumida do usuário. Não repetir o trabalho já entregue nem iniciar publicação por conta própria.
