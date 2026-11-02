# MangaFlow Browser — V0.1.1

Navegador Android para leitura vertical de mangás/manhwas com GeckoView, uma WebExtension embutida, favoritos locais, modo leitor e rolagem automática.

## Identidade permanente do aplicativo

- `applicationId`: `com.mangaflow.browser` — **não mudar**.
- Versão inicial assinada: `versionCode 1`, `versionName 0.1.1`.
- A cada nova versão aumentar `versionCode` e manter **exatamente a mesma chave de assinatura**.
- Evite instalar APK `debug` distribuído por CI: sua assinatura é temporária e poderá impedir atualizações por cima.
- Atualizações preservam dados locais quando instaladas sobre o mesmo aplicativo com chave compatível. Sempre faça backup antes de grandes mudanças.

## GitHub Actions — assinatura permanente

1. Abra o repositório → **Settings → Secrets and variables → Actions**.
2. Cadastre **MF_SIGNING_KEYSTORE_BASE64** com o conteúdo integral do arquivo `KEYSTORE_BASE64.txt` do pacote de assinatura privado.
3. Cadastre **MF_SIGNING_PASSWORD** com a senha do arquivo `SENHA_PRIVADA.txt`.
4. Execute **Actions → MangaFlow Android — APK assinado → Run workflow**.
5. Baixe o artifact **MangaFlow-v0.1.1-APK-assinado** e instale `app-release.apk`.

**NUNCA suba o .jks, os arquivos de senha ou qualquer cópia desses secrets ao repositório, mesmo se ele for privado.** Faça um backup seguro da chave. Perder a chave impede atualizar por cima das versões assinadas por ela.

## Funcionalidades e limitações

- GeckoView 159; bloqueio antirrastreamento e lista conservadora de domínios publicitários.
- Pop-ups automáticos negados; modo leitor pode esconder elementos de determinados sites.
- Rolagem automática 10–160 CSS px/s, pausa ao tocar, tela ativa durante reprodução.
- Favoritos, última leitura e progresso armazenados apenas localmente no dispositivo.
- Bloqueio de anúncios não é absoluto; não é um clone do uBlock Origin.
- Sem download offline, sem gerenciamento de abas e sem sincronização na nuvem nesta versão.

## Build

Java 17; Gradle 9.3.1; AGP 9.1.1; compileSdk 37; minSdk 26; GeckoView 159.0.20261022150525. A primeira execução do CI depende da criação dos dois secrets de assinatura.
