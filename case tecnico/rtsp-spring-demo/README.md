# Exemplo executável RTSP → Spring Boot → HLS / WebRTC

**Escopo:** laboratório local Windows + Docker Desktop + Java 17 + Maven + Node 20+ + FFmpeg (com `libx264` e `libopus`).

## Arquitetura

- `FFmpeg Windows` gera uma câmera virtual RTSP, enviando-a ao `origin` em `rtsp://localhost:8554/camera1`.
- **HLS:** o Spring Boot executa outro processo FFmpeg, converte vídeo para H.264 e áudio para AAC e disponibiliza `.m3u8` e `.ts` por endpoints REST.
- **WebRTC:** o Spring Boot provisiona dinamicamente `camera-1` na Control API do MediaMTX `gateway`, que recebe RTSP da `origin` e publica WebRTC. Nesta demonstração, a página WebRTC é servida pelo gateway (não pela JVM).
- Vue utiliza `hls.js` para HLS e iframe do player nativo MediaMTX para WebRTC.

## Preparação no Windows (PowerShell)

1. Instale Docker Desktop, FFmpeg, Java 17, Maven e Node.js.
2. Na pasta `infra`, execute `docker compose up -d`.
3. Abra outro PowerShell e execute a câmera virtual **em uma linha**:

```powershell
ffmpeg -re -f lavfi -i "testsrc2=size=1280x720:rate=25" -f lavfi -i "sine=frequency=440:sample_rate=48000" -c:v libx264 -preset ultrafast -tune zerolatency -pix_fmt yuv420p -g 50 -c:a libopus -ar 48000 -ac 2 -b:a 96k -f rtsp -rtsp_transport tcp rtsp://localhost:8554/camera1
```

4. Abra outro terminal na pasta `backend` e execute `mvn spring-boot:run`.
5. Na pasta `frontend` execute `npm install` e `npm run dev`.
6. Abra `http://localhost:5173` e clique em **Iniciar HLS** ou **Iniciar WebRTC**.

### Endpoints

```text
POST http://localhost:8080/api/cameras/1/hls/start
GET  http://localhost:8080/api/cameras/1/hls/index.m3u8
POST http://localhost:8080/api/cameras/1/hls/stop
POST http://localhost:8080/api/cameras/1/webrtc/provision
```

Na primeira execução, espere alguns segundos para o FFmpeg produzir os segmentos HLS. WebRTC precisa de conexão ICE na porta UDP 8189. Confira `docker compose logs gateway` para diagnóstico.

### Câmara RTSP real

Substitua `RTSP_URL` (visto pelo Spring no host) e `GATEWAY_RTSP_URL` (visto pelo gateway Docker). Exemplo em PowerShell:

```powershell
$env:RTSP_URL='rtsp://usuario:senha@192.168.1.100:554/stream1'
$env:GATEWAY_RTSP_URL='rtsp://usuario:senha@192.168.1.100:554/stream1'
mvn spring-boot:run
```

### Limitações importantes

**É uma demonstração de laboratório, não um serviço pronto para produção.** Não há Spring Security, autenticação para streams, isolamento de usuários, controle de espectadores, criptografia de credenciais, reconciliação completa da Control API, tratamento de restart dos processos FFmpeg, TTL, TLS, TURN/STUN e suporte multi-câmera. Configurações de controle e URL do gateway são acessíveis apenas localmente pelo Docker host. A URL `camera-1` é fixa. A API MediaMTX é exposta no loopback para simplificar testes locais; em produção, deixe-a privada. O browser precisa acessar a engine WebRTC e uma porta ICE de mídia; para expor tudo no mesmo domínio, use um reverse proxy/ingress e configuração ICE adequada. Credenciais RTSP em linha de comando podem ser vistas na lista de processos. Não exponha este exemplo na Internet.

**WebRTC não é apenas redirecionar bytes do FFmpeg para um controller.** Para ter a JVM como servidor WebRTC, implemente uma engine compatível com ICE/DTLS/SRTP ou integre uma biblioteca nativa; aqui o MediaMTX cumpre essa função, sob controle do Spring Boot.

Os codecs do áudio/vídeo precisam ser compatíveis; o stream sintético utiliza H.264 + Opus, enquanto HLS os transcodifica para H.264 + AAC.

## Referências

- https://mediamtx.org/docs/features/control-api
- https://mediamtx.org/docs/read/web-browsers
- https://www.ffmpeg.org/ffmpeg-formats.html
