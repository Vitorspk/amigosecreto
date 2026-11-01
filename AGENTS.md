# Trabalho neste repositório

- Antes de qualquer sessão de redesign, leia `docs/REDESIGN_PLAN.md`. Ele é a fonte de verdade do escopo, ordem, pendências e evidências.
- Siga a primeira etapa pendente. Registre qualquer mudança de escopo ou ordem no plano antes de executá-la.
- Ao encerrar cada sessão, atualize o registro, o estado das etapas, as verificações e o próximo passo. Use `done` somente para entregas implementadas e validadas pelos critérios do plano.
- Preserve alterações preexistentes do usuário. Trabalhe em branch própria; não faça commits diretamente na `master`.
- Execute `:app:testDebugUnitTest` e `:app:lintRelease` antes de abrir PR; para mudanças de interface, compile também `:app:assembleDebug`.
- A referência de implementação visual é `docs/REDESIGN_DESIGN_SYSTEM.md`. Preserve os fluxos e os dados existentes.
