# IconBro — análise de compatibilidade Android 14 (experimental)

## Fontes analisadas

- Iconify v8.0.0: tag `v8.0.0`, commit `1aad7e414ade5c5af8a057004ee24dbae366fe26`.
- Iconify v7.3.0: tag `v7.3.0`, commit `ee491e884487959d364f07797c60adcd984ea948`.
- A release pública mais recente consultada é v8.0.0. O branch oficial muda separadamente; o fork não faz sync automático de commits que possam abandonar suporte ao Android 14.
- O código-fonte completo das duas tags foi obtido e comparado. v8 tem `ModuleBase/`, fluxo de UI refeito e novos serviços AIDL; v7.3.0 não tem `ModuleBase/` e usa arquitetura anterior.

## Constatações diretas no código

- v8 configura compile SDK 36.1, minSdk 36 e targetSdk 36. Seu README recomenda v7.3.0 para Android 12 até Android 16 não-QPR e v8.0.0 para Android 16 QPR1 em diante.
- `ModuleBase/customize.sh` de v8 aborta instalação quando `ro.build.version.sdk` é inferior a 36. Isso impede o uso do módulo de sistema de v8 em Android 14 (API 34), independentemente de o APK ser compilável com minSdk 34.
- v7.3.0 configura compileSdk 35, minSdk 31 e targetSdk 35. O README da v8 indica v7.3.0 para Android 14, mas isso não comprova compatibilidade com ROM Motorola stock nem KernelSU em Moto G24.
- As duas versões são projetos Android nativos com Gradle/Kotlin e recursos/integração para root e Xposed. v8 alterou estrutura, catálogo de dependências, manifest, fluxos de serviço e bases de recursos; não é seguro importar em bloco a implementação anterior.
- v8 ainda declara namespace interno `com.drdisagree.iconify`. Ele é mantido para evitar quebrar referências/reflexões e integração com os hooks; identidade de instalação é separada por `applicationId`.
- O manifest v8 tem provider remoto baseado em `${applicationId}`, provider meteorológico com autoridade literal e permissão de weather literal. No IconBro a autoridade e permissão literal foram individualizadas para evitar colisão de provider/permission ao instalar junto do Iconify; a autoridade consumida pelo provider no código também foi atualizada.

## Matriz de portabilidade e testes

| Funcionalidade/área | v7.3.0 | v8.0.0 | Alteração IconBro | Compatibilidade API 34 | ROM/fabricante | Testes e limitações |
|---|---|---|---|---|---|---|
| Instalação do app | minSdk 31, target 35 | minSdk/target 36; compile 36 | applicationId pretendido `dev.iconbro`; min/target 34; namespace interno conservado | Não comprovada funcionalmente; a mudança de minSdk não prova execução | Em princípio não garante suporte Motorola | Sem JDK/SDK/aparelho nesta sessão; APK não compilado |
| Systemless module/root | Implementação anterior; não possui `ModuleBase/` na tag | `ModuleBase/customize.sh` verifica SDK >=36 antes de instalar; integra provider/app, root e LSPosed | O fluxo do onboarding e o instalador do fork aceitam API 34; outras versões abaixo de API 36 continuam bloqueadas | **Fluxo de instalação liberado em API 34** no fork; build e uso em dispositivo ainda precisam ser verificados | Root KernelSU e metamodule variam; integração depende também do framework Xposed/ROM | Não executado. API 34 liberada como opção experimental; requer teste de dispositivo |
| RRO/overlays e geração | Contém assets CompileOnDemand antigos; número/estrutura diferem | Assets CompileOnDemand atualizados, estrutura do módulo e fluxo de geração novos | Backport das implementações v7.3.0 com UI baseada na v8; assets QSLT/QSDT restaurados para Android 14, uso experimental | Indeterminado; nomes/recursos do SystemUI variam e não foram exercitados em API 34 nesta sessão | Forte dependência AOSP/Pixel e fabricante | Sem instalação/aplicação de overlay em device |
| Xposed / SystemUI | Manifest e código de hooks da arquitetura v7 | Hooks atuais e serviço AIDL em nova estrutura; module scope é parte da configuração | Identificadores de classes internos não renomeados | Não confirmado; hooks podem referir recursos/implementações posteriores | Depende de LSPosed/Vector, SystemUI e ROM; não presume Motorola compatível | Sem runtime LSPosed em ambiente |
| QPR1 / API 36 | Base anterior às mudanças de v8 | README e scripts declaram suporte mínimo de Android 16 QPR1/API 36 | Bloqueio mantido, sem alegar backport | Não se aplica a API 34 | Plataforma AOSP/Pixel citada pelo upstream | Confirmado pela leitura do README e script, não por execução |
| Identidade / coexistência | `com.drdisagree.iconify` | `com.drdisagree.iconify` | applicationId `dev.iconbro`; authority/permission weather `dev.iconbro.*`; namespace de código permanece upstream; diretório/id de módulo IconBro | O app deve ter package distinto; a instalação em Android ainda não foi verificada | Integração de hooks ainda pode ter limitações | Revisão de código realizada; pacote final ainda não pode ser inspecionado sem APK |
| Colisão entre overlays de ambos os forks | Componentes `IconifyComponent*` | Componentes `IconifyComponent*` | Identificadores internos foram mantidos; renomeá-los em bloco exigiria reavaliar recursos, scripts e hooks | Não deve ser considerado coexistência segura de módulos | Potencial colisão em overlay manager do Android e efeitos que usam os mesmos alvos | Não testado; instalar o APK junto é diferente de ativar módulos/overlays simultaneamente. Não ative os dois conjuntos de overlays até separação e teste.

## Resultado honesto nesta etapa

O repositório é um fork identificado e preparado com workflow, documentação e alterações de identidade, mas **não é ainda um backport funcional comprovado para Android 14**. Não foi possível executar build, testes, inspeção de APK, instalação ou testes em emulador/aparelho: este ambiente não contém `git`, Java/JDK, Gradle instalado, Android SDK, `aapt`/`apkanalyzer` nem `apksigner`. O wrapper Gradle está no código, mas não há JVM para executá-lo. Tampouco houve acesso a um Moto G24.

O fork permite explicitamente API 34 no onboarding e no instalador do módulo; a Home pode ser aberta sem instalar. As implementações API 34 portadas e assets de tema foram adicionados, mas ainda precisam de build/runtime em dispositivo. A comparação com versões futuras do upstream deve ser feita por recursos: não usar o atualizador upstream para instalar releases do Iconify no IconBro. O mecanismo BootLoopProtector upstream ignora o carregamento dos hooks após falhas repetidas no mesmo processo; ele não garante a desativação de overlays ou de outros módulos root. O build e testes em dispositivo seguem pendentes nesta sessão. O workflow proposto é automatizado, mas sua primeira execução e publicação precisam acontecer num repositório GitHub com Actions habilitado.
