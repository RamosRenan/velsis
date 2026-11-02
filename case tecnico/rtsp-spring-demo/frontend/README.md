# Executar o player Vue + Spring

## O que foi corrigido

- Plugin Vue e configuração do Vite para compilar `App.vue`.
- Proxy `/api` para o Spring, incluindo playlists e segmentos HLS, sem depender de CORS no navegador.
- Espera de até 45 segundos pela playlist com segmentos, em vez de atraso fixo de três segundos.
- Cancelamento de inicializações antigas ao parar, tratamento de erros HTTP e de reprodução, autoplay e controles de áudio.
- Comandos de build e preview.

## Preparação

É necessário Node, Java 17+, Maven, Docker Desktop e FFmpeg com libx264 e libopus.
No terminal usado para iniciar o Spring, `ffmpeg -version` deve funcionar. Alternativamente, defina `FFMPEG_PATH` com o caminho completo do executável.

No ambiente analisado, o Spring respondeu HTTP 500 ao POST `/api/cameras/1/hls/start`, o FFmpeg não foi encontrado no PATH e `docker compose ps` não mostrou os serviços do projeto em execução. A reprodução ponta a ponta depende de resolver esses pré-requisitos.

## Inicialização (PowerShell)

Execute os comandos abaixo partindo desta pasta `frontend`, cada processo em um terminal separado.

1. Suba a origem RTSP e o gateway:

```powershell
docker compose -f ../infra/compose.yml up -d
```

2. Inicie a câmera virtual e mantenha o terminal aberto:

```powershell
ffmpeg -re -f lavfi -i "testsrc2=size=1280x720:rate=25" -f lavfi -i "sine=frequency=440:sample_rate=48000" -c:v libx264 -preset ultrafast -tune zerolatency -pix_fmt yuv420p -g 50 -c:a libopus -ar 48000 -ac 2 -b:a 96k -f rtsp -rtsp_transport tcp rtsp://localhost:8554/camera1
```

3. Inicie o Spring. `JAVA_HOME` deve apontar para a pasta do JDK, sem o sufixo `bin`. Neste computador:

```powershell
$env:JAVA_HOME='F:\desenvolvimento\ambiente\jdk-17.0.12'
# Caso o FFmpeg não esteja no PATH, ajuste para o local real:
# $env:FFMPEG_PATH='C:\ffmpeg\bin\ffmpeg.exe'
cd ../backend
mvn spring-boot:run
```

Se o Spring já estiver rodando pela IDE, configure `FFMPEG_PATH` no ambiente da execução e reinicie-o; não abra uma segunda instância na porta 8080.

4. Inicie o frontend:

```powershell
npm install
npm run dev
```

Abra http://localhost:5173 e clique em **Iniciar HLS**. Aguarde os primeiros segmentos. O player começa sem áudio; ative-o nos controles. HLS tem alguns segundos de atraso. Para menor latência, use **Iniciar WebRTC**; o navegador precisa alcançar o gateway em http://localhost:8890 e a porta UDP 8189.

Se a porta 5173 estiver ocupada, use o servidor já aberto ou encerre-o no terminal correspondente e execute novamente.

## Backend em outra porta

Copie `.env.example` para `.env.local`, ajuste `BACKEND_URL` e reinicie o Vite. Por padrão, o proxy aponta para `http://localhost:8080`.

## Câmera real

Antes de iniciar o Spring, configure `RTSP_URL` com a URL acessível pelo host e `GATEWAY_RTSP_URL` com a URL acessível pelo contêiner gateway. Com uma câmera real, não execute o gerador de câmera virtual.

## Diagnóstico

- HTTP 500 ao iniciar HLS: consulte o console do Spring; verifique o executável configurado em `FFMPEG_PATH`.
- Playlist não gerada: consulte `../backend/runtime-hls/camera-1/ffmpeg.log`, confirme a publicação RTSP e os contêineres.
- WebRTC sem vídeo: consulte `docker compose -f ../infra/compose.yml logs gateway` e as mensagens dentro do player.
- O botão Parar em HLS chama o endpoint de parada do Spring, compartilhado por todos os espectadores dessa câmera. Em WebRTC, fecha o player local.
- `npm run build` gera `dist`; `npm run preview` serve o resultado com o mesmo proxy local. Para hospedar `dist` em outro servidor, configure o encaminhamento de `/api` nesse servidor.
