# Streaming de câmeras (HLS)

Implementação em `com.velsis.monitoring.hardware.camStream`, migrada do demo sem WebRTC.

A API consulta a tabela `CAMERA` pelo nome exato cadastrado (diferencia maiúsculas e minúsculas).
O endereço RTSP é montado com `enderecoip`, `porta_rtsp`, `usuario`, `senha` e `nome`.
O campo `nome` também define o caminho RTSP: `camera1` gera
`rtsp://usuario:senha@IP:porta/camera1`. Cadastre o caminho que o equipamento disponibiliza.
Credenciais são codificadas na URL e não aparecem nas respostas ou logs da aplicação.
O FFmpeg precisa das credenciais nos argumentos do processo; restrinja o acesso ao usuário do servidor.

## Configuração

- `FFMPEG_PATH`: executável FFmpeg (padrão: `ffmpeg`), com suporte a libx264 e AAC.
- `STREAM_DIRECTORY`: diretório gravável dos arquivos HLS (padrão: `./runtime-hls`).

O caminho vem da coluna `CAMERA.nome`; não é necessário configurar `STREAM_RTSP_PATH`.

## API

Use o nome com codificação de segmento de URL, por exemplo `Porta%20principal`.
Todas as rotas usam a autenticação existente; POST exige o token CSRF da sessão.

- `POST /api/cameras/by-name/{nome}/hls/start`: inicia o FFmpeg ou reutiliza o processo ativo.
  Retorna `{"playlist":"/api/cameras/by-name/Entrada/hls/index.m3u8"}`.
- `GET /api/cameras/by-name/{nome}/hls/index.m3u8`: playlist.
- `GET /api/cameras/by-name/{nome}/hls/seg_00001.ts`: segmento.
- `POST /api/cameras/by-name/{nome}/hls/stop`: encerra o processo da câmera.

O início é assíncrono: a playlist pode retornar 404 enquanto o FFmpeg gera os primeiros segmentos.
Processo indisponível retorna 503; câmera ausente retorna 404; câmera inativa retorna 409.
Cada câmera possui um processo e diretório separados por ID interno, permitindo streams simultâneos.
Vários espectadores da mesma câmera compartilham o processo; parar o stream afeta todos eles.
Os processos são encerrados no desligamento do backend. Esta implementação mantém o controle em uma única instância do backend.

## Banco

A migração Liquibase `004-camera-unique-name` adiciona `UNIQUE(nome)`.
Se já houver nomes duplicados, ela interrompe com mensagem explicativa; renomeie os registros antes de reiniciar.
O cadastro responde 409 ao tentar inserir nome repetido. As migrações anteriores não foram alteradas.

## Validação

Testes com processos simulados cobrem credenciais, IPv6, isolamento, chamadas concorrentes,
reinício, encerramento, arquivos, erros e contrato HTTP. A conexão real depende de uma câmera
acessível, do caminho RTSP correto e do FFmpeg instalado.

