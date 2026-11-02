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

## Novidades da V0.2.0 — Rolagem por etapas

- **Contínua:** de 10 a 160 px/s com ajuste pela barra.
- **Por etapas:** avança cerca de 65% da tela a cada 2, 3, 5 ou 8 segundos.
- Para configurar, toque no número da velocidade ou mantenha o botão **Auto** pressionado.
- Toque na página para pausar a rolagem; ela também para ao fim do capítulo, considerando imagens ainda em carregamento.
- A retomada de leitura espera o conteúdo carregar para não sobrescrever imediatamente o progresso salvo.
- Favoritos, preferências, pacote Android e chave de assinatura foram preservados.

**Teste no celular:** abra um capítulo longo, ative Leitor, escolha o modo por etapas, depois feche e reabra o app para verificar continuidade; teste também o bloqueio de anúncios em seus sites preferidos. A compilação pelo CI não substitui esse teste manual.

## V0.3.0 — Melhoria de bloqueio (NeoToons)

- Adicionado `ad-cleaner.js`, ativo desde o início do carregamento, sem precisar ativar o modo Leitor.
- Detecta de forma conservadora o overlay flutuante "TESTE GRÁTIS / IPTV" visto no NeoToons e anúncios com nomes/hosts conhecidos.
- `MutationObserver` remove elementos publicitários inseridos depois de carregar a página, sem varreduras contínuas durante a rolagem.
- Protege painéis do capítulo, imagens do leitor e navegação. Não oculta indiscriminadamente `modal`, `sticky`, `banner` nem todas as imagens.
- Atualiza a versão da WebExtension para garantir sua reinstalação no GeckoView após atualizar o APK.
- Não altera a identidade do app nem a assinatura existente; instala por cima com `versionCode` crescente.
- Testes de sintaxe e regressão do bloqueador rodam antes de compilar o APK.

**Limitação:** um anúncio desenhado dentro de uma imagem do próprio capítulo não pode ser removido com segurança usando apenas filtragem do DOM; requer identificação confiável do arquivo ou controle manual para não apagar quadros legítimos.
