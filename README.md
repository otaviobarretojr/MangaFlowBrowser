# MangaFlow Browser

Navegador Android para ler mangás e manhwas com GeckoView, bloqueio inicial de publicidade, modo leitor, favoritos locais e rolagem automática.

## V0.1.1 — base Android e identidade visual
O projeto-fonte V0.1.1 foi preparado com ícone próprio e configuração de APK release assinado. Os arquivos ainda precisam ser publicados neste repositório, preservando a estrutura de pastas.

### Atualização por cima sem perder dados
- O `applicationId` deve permanecer `com.mangaflow.browser`.
- Cada nova versão deve aumentar `versionCode` (1, 2, 3...), não apenas `versionName`.
- A assinatura deve ser sempre feita pela **mesma chave privada** da primeira versão distribuída.
- É obrigatório configurar os segredos do GitHub Actions: `MF_SIGNING_KEYSTORE_BASE64` e `MF_SIGNING_PASSWORD`.
- **Nunca publique o arquivo JKS, a senha ou a chave em commits, mesmo em repositório privado.**

### Estado
- Identidade visual gerada e incorporada ao pacote fonte V0.1.1.
- Projeto validado estaticamente (XML/JavaScript/ZIP), ainda **sem build Android confirmado**.
- Primeiro APK assinado dependerá dos secrets e de um workflow Android concluído com sucesso.

Este repositório é público no momento; preserve rigorosamente a privacidade do kit de assinatura.
