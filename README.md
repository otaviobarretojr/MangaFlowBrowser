# Túnel Pessoal — Android

Aplicativo Android simples para uma VPN WireGuard pessoal, sem anúncios, login ou telemetria. Baseado na biblioteca oficial `com.wireguard.android:tunnel:1.0.20260102` (Apache 2.0).

## O que faz
- Importa um arquivo WireGuard `.conf` do dispositivo.
- Solicita a permissão de VPN do Android.
- Liga/desliga o túnel com um toque.
- Salva a configuração criptografada com Android Keystore AES-GCM.
- Trabalha com um perfil de cada vez, sem enviar configurações a serviços próprios.

## O que NÃO faz
- Não fornece servidor VPN, assinatura, IP público ou perfil automaticamente.
- Não garante acesso em redes corporativas que bloqueiam túneis VPN.
- Não finge que a conexão está funcionando só porque o túnel foi ativado.
- Não contém função de ofuscação ou disfarce de tráfego.
- Não substitui auditorias, política de segurança ou monitoramento da rede.

## Como obter o APK pelo celular
Abra **Actions** > **Gerar APK Túnel Pessoal**, abra a execução mais recente e baixe o artefato `TunelPessoal-APK`. O ZIP contém `app-debug.apk` pronto para instalar no Android.

## Como conectar
1. Tenha acesso autorizado a um servidor WireGuard e obtenha dele um arquivo `.conf`.
2. Instale o APK, importe o perfil e toque em **CONECTAR**.
3. Autorize a VPN no aviso do Android.
4. Verifique acesso e IP externo. A mensagem “túnel ativado” não confirma que o servidor responde.

## Builds
- Android Gradle Plugin 8.10.1, Gradle 8.13, JDK 17, API 36.
- O workflow gera APK debug assinado. Para atualização por cima, o mesmo certificado deve ser reutilizado. O workflow usa cache de keystore debug, que pode expirar; para distribuição duradoura, configure uma keystore de release mantida em segredo.
- O serviço VPN é fornecido e declarado pelo manifesto da biblioteca oficial WireGuard.
- Projeto V1: reconexão após o processo ser encerrado e indicador de handshake ainda não são implementados.

## Aviso
Use a VPN apenas em redes e contextos onde tenha autorização. O tráfego vai passar pelo servidor VPN que você selecionar; escolha um operador de confiança.
