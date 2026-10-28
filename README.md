# IconBro

[![Unofficial fork](https://img.shields.io/badge/project-unofficial%20community%20fork-blue)](https://github.com/in3zalo-gk/IconBro)

**IconBro is an independent community fork of Iconify**, maintained at [in3zalo-gk/IconBro](https://github.com/in3zalo-gk/IconBro). The repository currently appears as a standalone GitHub repository rather than a GitHub-marked fork; the source lineage and upstream license are documented here. It is not affiliated with or endorsed by the original authors.

Fork comunitário **não oficial** do Iconify, mantido como projeto independente. A base principal é Iconify v8.0.0; implementações Android 14 foram selecionadas da v7.3.0 quando necessário. Os créditos de código e a licença original GPL-3.0 são preservados. Não é afiliado nem endossado pelos autores do projeto original.

> **Aviso importante:** o fluxo de instalação do IconBro foi liberado para Android 14 (API 34), além do Android 16 QPR1/API 36+. A compatibilidade depende da ROM, do root e do LSPosed; ainda não há validação independente neste ambiente. O Iconify possui proteção de bootloop para os hooks: após falhas repetidas, deixa de carregá-los. Isso não garante recuperação de problemas causados por overlays, root ou outros módulos.

## Situação

- Base principal: Iconify v8.0.0 (última versão oficial publicada consultada), commit `1aad7e414ade5c5af8a057004ee24dbae366fe26`.
- Referência de comparação: Iconify v7.3.0, commit `ee491e884487959d364f07797c60adcd984ea948`.
- O próprio README upstream direciona Android 12–Android 16 não-QPR para v7.3.0 e Android 16 QPR1+ para v8.0.0; este fork libera experimentalmente o fluxo em API 34.
- Identidade planejada do APK: `dev.iconbro`; namespace de código upstream é mantido para não quebrar hooks e referências internas. As authorities/permission meteorológicas foram isoladas para coexistência.
- Suporte Android 14: **experimental**; o instalador e o onboarding aceitam API 34. O funcionamento de cada recurso ainda depende de Android, ROM, fabricante, root e framework Xposed; não assumir compatibilidade universal com Motorola stock.
- O alvo de investigação é Moto G24, Android 14, firmware Motorola original e KernelSU. Não houve teste físico no dispositivo.
- Iconify é distribuído sob GPL-3.0; o arquivo `LICENSE` original foi mantido. Consulte `ANDROID14_COMPATIBILITY.md` para comparação, limites e testes que ainda faltam.

### O que foi e não foi validado

| Etapa | Estado |
|---|---|
| Código-fonte das tags e commits identificados | Feito |
| Comparação de build, manifest, estrutura e bloqueio do módulo | Feita por leitura do código |
| Alterações de identidade do fork | Aplicadas ao código; pacote final ainda não inspecionado em APK |
| Build local / APK / assinatura / checksum | Não executado: ambiente sem JDK e Android SDK |
| App abre / teste em emulador | Não testado |
| Recursos root, overlays ou Xposed | Não testados |
| Moto G24 firmware Motorola + KernelSU | Não testado |

## Baixar um APK Beta pelo GitHub Actions

No [repositório IconBro](https://github.com/in3zalo-gk/IconBro):

1. Abra a aba **Actions** do repositório.
2. Escolha uma execução concluída com sucesso do workflow **IconBro Android 14 Beta**.
3. Na seção **Artifacts**, baixe **IconBro-Android14-Beta**.
4. Extraia o ZIP e confira o `SHA256SUMS.txt`.
5. O fluxo de instalação do módulo aceita API 34, mas trata o suporte Android 14 como experimental. Antes de aplicar mudanças de sistema, faça backup e confirme que sabe desativar o módulo pelo seu gerenciador de root/recovery.

O workflow publica APK de teste assinado com a chave debug do Android (não é uma assinatura para distribuição estável). Uma tag `iconbro-vX.Y.Z-beta.N` também dispara release marcado como Pre-release, apenas se build e verificações passarem.

## Recuperação / segurança

A pipeline só compila e publica artefatos; não modifica o telefone. Não aplique overlays nem altere SystemUI em dispositivo principal sem backup e procedimento de desativação do módulo/root já conhecido. Se uma futura versão validada causar bootloop, use o modo seguro ou desative/remova o módulo pelo mecanismo de root/recovery específico do aparelho.
