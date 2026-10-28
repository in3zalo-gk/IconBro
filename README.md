# IconBro

Fork comunitário **não oficial** do [Iconify](https://github.com/Mahmud0808/Iconify), com identidade independente e investigação experimental de compatibilidade com Android 14. Não é afiliado nem endossado pelos autores do Iconify.

> **Aviso importante:** isto ainda não é um backport funcional validado para Android 14. A base v8.0.0 requer Android 16 QPR1/API 36 e o script original do módulo aborta abaixo da API 36. O guard foi mantido; não tente instalar o módulo de sistema no Moto G24/API 34. Apenas baixar ou compilar um APK não demonstra compatibilidade funcional.

## Situação

- Base principal: Iconify v8.0.0, commit `1aad7e414ade5c5af8a057004ee24dbae366fe26`.
- Referência de comparação: Iconify v7.3.0, commit `ee491e884487959d364f07797c60adcd984ea948`.
- O próprio README upstream direciona Android 12–Android 16 não-QPR para v7.3.0 e Android 16 QPR1+ para v8.0.0.
- Identidade planejada do APK: `dev.iconbro`; namespace de código upstream é mantido para não quebrar hooks e referências internas. As authorities/permission meteorológicas foram isoladas para coexistência.
- Suporte Android 14: **experimental e não comprovado**. Funcionalidades podem estar indisponíveis ou falhar conforme Android, ROM, fabricante, root e framework Xposed. A Motorola stock não é assumida compatível.
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

Depois de publicar este repositório no GitHub e habilitar Actions:

1. Abra a aba **Actions** do repositório.
2. Escolha uma execução concluída com sucesso do workflow **IconBro Android 14 Beta**.
3. Na seção **Artifacts**, baixe **IconBro-Android14-Beta**.
4. Extraia o ZIP e confira o `SHA256SUMS.txt`.
5. Instale o APK em Android apenas para avaliação do app. Não instale/ative módulo de sistema no Android 14 enquanto o backport não for validado.

O workflow publica APK de teste assinado com a chave debug do Android (não é uma assinatura para distribuição estável). Uma tag `iconbro-vX.Y.Z-beta.N` também dispara release marcado como Pre-release, apenas se build e verificações passarem.

## Recuperação / segurança

A pipeline só compila e publica artefatos; não modifica o telefone. Não aplique overlays nem altere SystemUI em dispositivo principal sem backup e procedimento de desativação do módulo/root já conhecido. Se uma futura versão validada causar bootloop, use o modo seguro ou desative/remova o módulo pelo mecanismo de root/recovery específico do aparelho.
