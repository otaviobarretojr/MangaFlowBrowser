# MangaFlow Browser

Navegador Android especializado em leitura de mangás e manhwas.

## Criar um APK instalável e atualizável

1. Em **Settings → Secrets and variables → Actions**, mantenha somente o segredo obrigatório `MF_SIGNING_PASSWORD` (pelo menos 6 caracteres, idealmente uma senha forte). O segredo antigo `MF_SIGNING_KEYSTORE_BASE64` não é mais usado.
2. Em **Actions → MangaFlow Android — APK assinado → Run workflow**, execute o processo.
3. Na **primeira execução**, o GitHub Actions gera uma chave permanente, criptografa-a com a senha secreta e grava apenas o arquivo `signing/mangaflow-release.jks.enc` no repositório.
4. O mesmo workflow monta o APK release e publica o artifact **MangaFlow-APK-Assinado**.
5. Nas próximas execuções, ele descriptografa a **mesma chave**, preserva o `applicationId` `com.mangaflow.browser`, incrementa automaticamente o `versionCode` e gera um APK que pode instalar por cima.

**Atenção:** faça backup do arquivo `signing/mangaflow-release.jks.enc` e guarde em segurança o valor do secret `MF_SIGNING_PASSWORD`. Sem essa senha não será possível recuperar a chave de assinatura para futuras atualizações. Não revele a senha nem publique a keystore sem criptografia. O repositório pode continuar público.

### Projeto
- Motor GeckoView, bloqueio inicial de anúncios/pop-ups, leitor imersivo e rolagem automática.
- Java 17, Gradle 9.3.1, Android Gradle Plugin 9.1.1, compileSdk 37.
- APK assinado disponível na aba **Actions**, quando a compilação terminar com sucesso.
