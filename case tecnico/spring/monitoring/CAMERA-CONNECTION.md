# Verificacao de operacao das cameras

Uma camera habilitada e ONLINE somente quando o ping ICMP ao IP cadastrado tem sucesso E o FFmpeg recebe e decodifica ao menos um frame do RTSP. Se qualquer etapa falhar, fica OFFLINE. Uma porta TCP aberta nao basta. Cameras desabilitadas no cadastro ficam DISABLED e nao sao verificadas.

O monitor inicia com a aplicacao e espera 30 segundos apos cada ciclo. Usa executor dedicado e verifica cameras sequencialmente. O status manual de cadastro nao e sobrescrito.

Configuracao Spring:
- monitoring.camera.interval-ms: intervalo apos cada ciclo, padrao 30000, minimo 1000.
- monitoring.camera.ping-timeout-ms: timeout do ping, padrao 3000 (1 a 30000); processo tem tolerancia adicional de 1 segundo.
- monitoring.camera.frame-timeout-ms: limite para obter um frame, padrao 10000 (1 a 60000).
- stream.ffmpeg / FFMPEG_PATH: executavel FFmpeg, o mesmo utilizado pelo HLS.

Requer ping instalado, ICMP permitido e FFmpeg disponivel no servidor. Usa a mesma URL RTSP do HLS, com IP, porta, credenciais e nome da camera como caminho. Falhas das ferramentas tambem impedem confirmar operacao. A imagem temporaria e removida apos a verificacao; processos que excedem o prazo sao encerrados.

Endpoints autenticados:
- GET /api/cameras/connections: ultimo resultado de todas as cameras.
- GET /api/cameras/{id}/connection: ultimo resultado individual; 404 se inexistente.
- POST /api/cameras/{id}/connection/check: verifica imediatamente; exige CSRF habitual.
- GET /api/cameras/inoperative: alertas das cameras com ultimo resultado OFFLINE.

Resposta: id, nome, state e checkedAt (UTC). UNKNOWN significa ainda nao verificada; checkedAt e null para UNKNOWN e DISABLED. ONLINE confirma ping e frame. OFFLINE significa falha na verificacao.

Resultados ficam em memoria e voltam a UNKNOWN no reinicio. checkedAt permite avaliar a idade da leitura; uma falha de banco preserva a ultima leitura e gera log. A tela deve consumir os novos endpoints para mostrar operacao separada de Ativa/Inativa. Nenhuma credencial e retornada.